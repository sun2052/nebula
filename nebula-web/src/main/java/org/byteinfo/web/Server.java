package org.byteinfo.web;

import org.byteinfo.context.Context;
import org.byteinfo.logging.Log;
import org.byteinfo.util.function.CheckedConsumer;
import org.byteinfo.util.function.Unchecked;
import org.byteinfo.util.misc.Config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Server extends Context {
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	private final List<CheckedConsumer<Server>> onStartHandlers = new ArrayList<>();
	private final List<CheckedConsumer<Server>> onStopHandlers = new ArrayList<>();

	// HTTP Handlers: path -> (method -> handler)
	private final Map<String, Map<String, Handler>> exactHandlers = new HashMap<>();
	private final Map<String, Map<String, Handler>> genericHandlers = new LinkedHashMap<>();

	// HTTP Filters
	private final List<Filter> filters = new ArrayList<>();

	// Result Encoders: result type -> result encoder
	private final Map<Class<?>, Encoder> encoders = new HashMap<>();

	// HTTP Security Attributes
	private final Map<Handler, String> securityAttributes = new HashMap<>();

	// Global Error Handler
	private ErrorHandler errorHandler = ErrorHandler.DEFAULT;

	// HTTP Asset Handler
	private final Handler assetHandler;

	private final long startTime;
	private volatile ServerSocket serverSocket;
	private volatile boolean started;

	public Server(Object... modules) throws IOException {
		super(modules);
		startTime = System.currentTimeMillis();
		Log.info("Initializing Server...");
		Log.info("JVM: name={}, version={}, pid={}", System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), ProcessHandle.current().pid());
		Log.info("OS: name={}, arch={}, version={}", System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));

		// load configurations
		Config config = AppConfig.get();
		String configFile = "org/byteinfo/web/application.properties";
		config.load(configFile);
		Log.info("Loading default config: " + configFile);

		configFile = "application.properties";
		if (config.loadOptional(configFile)) {
			Log.info("Loading custom config: " + configFile);
		}

		// init runtime variables
		int processors = Runtime.getRuntime().availableProcessors();
		config.load(Map.of(
				"runtime.pid", ProcessHandle.current().pid(),
				"runtime.processors", processors
		));

		// system properties have the highest priority
		config.load(System.getProperties());

		// resolve all variables
		config.resolveAllVariables();

		// init asset handler
		assetHandler = new AssetHandler();
	}

	public Server start() throws Exception {
		if (started) {
			return this;
		}
		started = true;

		// start server socket
		Config config = AppConfig.get();
		int port = config.getInt("http.port");
		int backlog = config.getInt("http.backlog");
		int bufferSize = config.getInt("tcp.bufferSize");
		InetAddress bindAddr = InetAddress.getByName(config.get("http.bindAddr"));
		serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		serverSocket.setReceiveBufferSize(bufferSize);
		serverSocket.bind(new InetSocketAddress(bindAddr, port), backlog);
		Thread main = new Thread(Unchecked.runnable(() -> {
			AtomicLong counter = new AtomicLong();
			while (started) {
				Socket socket = serverSocket.accept();
				executor.execute(() -> {
					long id = counter.incrementAndGet();
					try (socket) {
						Log.debug("Connection {} Established: {}", id, socket);
						socket.setTcpNoDelay(true);
						socket.setSendBufferSize(bufferSize);
						socket.setSoTimeout(HttpContext.SESSION_TIMEOUT);
						handleConnection(socket, id);
						Log.debug("Connection {} Terminated.", id);
					} catch (EOFException e) {
						Log.debug("Connection {} Terminated: Closed by remote peer.", id);
					} catch (SocketTimeoutException e) {
						Log.debug("Connection {} Terminated: Socket timed out.", id);
					} catch (Exception e) {
						Log.error(e, "Connection {} Terminated: Uncaught Exception:", id);
					}
				});
			}
		}));
		for (CheckedConsumer<Server> handler : onStartHandlers) {
			handler.accept(this);
		}
		main.setName(getClass().getSimpleName() + "-" + port);
		main.start();
		Log.info("Server started in {} ms: http://127.0.0.1:{}", System.currentTimeMillis() - startTime, port);
		return this;
	}

	public Server stop() throws IOException {
		if (started) {
			started = false;
			serverSocket.close();
			executor.close();
			for (CheckedConsumer<Server> handler : onStopHandlers) {
				try {
					handler.accept(this);
				} catch (Exception e) {
					Log.warn(e);
				}
			}
		}
		Log.info("Server stopped.");
		return this;
	}

	public Server onStart(CheckedConsumer<Server> handler) {
		ensureNotStarted();
		onStartHandlers.add(handler);
		return this;
	}

	public Server onStop(CheckedConsumer<Server> handler) {
		ensureNotStarted();
		onStopHandlers.add(handler);
		return this;
	}

	public Server get(String path, Handler handler) {
		return handler(HttpMethod.GET, path, handler);
	}

	public Server post(String path, Handler handler) {
		return handler(HttpMethod.POST, path, handler);
	}

	public Server put(String path, Handler handler) {
		return handler(HttpMethod.PUT, path, handler);
	}

	public Server delete(String path, Handler handler) {
		return handler(HttpMethod.DELETE, path, handler);
	}

	public Server handler(String method, String path, Handler handler) {
		return handler(List.of(method), path, handler, null);
	}

	public Server handler(List<String> methods, String path, Handler handler, String securityAttribute) {
		ensureNotStarted();
		Map<String, Map<String, Handler>> handlers = exactHandlers;
		if (path.endsWith("*")) {
			path = path.substring(0, path.length() - 1);
			handlers = genericHandlers;
		}
		for (String method : methods) {
			handlers.computeIfAbsent(path, k -> new HashMap<>()).put(method, handler);
		}
		securityAttributes.put(handler, securityAttribute);
		return this;
	}

	public Server handler(Class<?>... classes) {
		ensureNotStarted();
		for (Class<?> clazz : classes) {
			Path basePath = clazz.getAnnotation(Path.class);
			String path = basePath == null ? "" : basePath.value();
			Secured baseSecured = clazz.getAnnotation(Secured.class);
			String secured = baseSecured == null ? null : baseSecured.value();
			for (Method method : clazz.getDeclaredMethods()) {
				List<String> httpMethods = new ArrayList<>();
				String currentPath = path;
				for (Annotation annotation : method.getDeclaredAnnotations()) {
					if (annotation instanceof Path) {
						currentPath += ((Path) annotation).value();
					}
					if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
						httpMethods.add(annotation.annotationType().getAnnotation(HttpMethod.class).value());
					}
				}

				MVCHandler mvcHandler = new MVCHandler(instance(clazz), method);
				Secured annotation = method.getAnnotation(Secured.class);
				String securityAttribute = annotation == null ? secured : annotation.value();
				handler(httpMethods, currentPath, mvcHandler, securityAttribute);
			}
		}
		return this;
	}

	public Server filter(Filter filter) {
		ensureNotStarted();
		filters.add(filter);
		return this;
	}

	public Server filter(Class<? extends Filter>... classes) {
		ensureNotStarted();
		for (Class<? extends Filter> clazz : classes) {
			filters.add(instance(clazz));
		}
		return this;
	}

	public Server error(ErrorHandler handler) {
		ensureNotStarted();
		errorHandler = handler;
		return this;
	}

	public Server error(Class<? extends ErrorHandler> clazz) {
		return error(instance(clazz));
	}

	public Server encoder(Class<?> type, Encoder handler) {
		ensureNotStarted();
		encoders.put(type, handler);
		return this;
	}

	public Server encoder(Class<?> type, Class<? extends Encoder> clazz) {
		return encoder(type, instance(clazz));
	}

	private void ensureNotStarted() {
		if (started) {
			throw new IllegalStateException("Server has already started.");
		}
	}

	private void handleConnection(Socket socket, long connectionId) throws Exception {
		InputStream in = new BufferedInputStream(socket.getInputStream());
		OutputStream out = new BufferedOutputStream(socket.getOutputStream());
		AtomicLong counter = new AtomicLong();
		while (true) {
			HttpContext ctx = null;
			Handler handler = null;
			Object result = null;
			Throwable th = null;
			try {
				// parse request
				String contextId = connectionId + "#" + counter.incrementAndGet();
				ctx = new HttpContext(contextId, socket, out, encoders, HttpCodec.parseRequest(in));
				Log.debug("{}: {} {}://{}{} IP={}", ctx.id(), ctx.method(), ctx.scheme(), ctx.host(), ctx.target(), ctx.address());

				// search for handler: exact handler > generic handler > asset handler
				handler = exactHandlers.getOrDefault(ctx.path(), Map.of()).get(ctx.method());
				if (handler == null) {
					for (Map.Entry<String, Map<String, Handler>> entry : genericHandlers.entrySet()) {
						if (ctx.path().startsWith(entry.getKey())) {
							handler = entry.getValue().get(ctx.method());
							break;
						}
					}
				}
				if (handler != null) {
					ctx.setSecurityAttribute(securityAttributes.get(handler));
				}
				if (handler == null) {
					handler = assetHandler;
				}

				// apply before filters
				for (Filter filter : filters) {
					filter.before(ctx, handler);
					if (ctx.isCommitted()) {
						break;
					}
				}

				// handle request
				if (!ctx.isCommitted()) {
					result = handler.handle(ctx);

					// apply after filters
					for (Filter filter : filters.reversed()) {
						filter.after(ctx, handler, result);
						if (ctx.isCommitted()) {
							break;
						}
					}
				}
			} catch (Exception e) {
				th = e;
				if (e instanceof InvocationTargetException ex && ex.getCause() != null) {
					th = ex.getCause();
				}
				if (th instanceof EOFException eof) {
					throw eof;
				}
				if (ctx == null) {
					try {
						HttpCodec.send(out, StatusCode.BAD_REQUEST);
					} catch (Exception ex) {
						// ignore
					}
					throw e;
				} else {
					try {
						result = errorHandler.handle(ctx, th);
					} catch (Exception ex) {
						Log.error(e, "Failed to apply error handler: {}: {} {}://{}{} IP={}, UA={}", ctx.id(), ctx.method(), ctx.scheme(), ctx.host(), ctx.target(), ctx.address(), ctx.userAgent());
					}
				}
			} finally {
				if (ctx != null) {
					// send response
					if (!ctx.isCommitted()) {
						ctx.commit(result);
					}
					Log.debug("{}: {} {}", ctx.id(), ctx.responseStatus(), ctx.responseLength());

					// apply complete filters
					for (Filter filter : filters) {
						try {
							filter.complete(ctx, handler, th);
						} catch (Exception e) {
							Log.error(e, "Failed to apply filter: {}: {} {}://{}{} IP={}, UA={}", ctx.id(), ctx.method(), ctx.scheme(), ctx.host(), ctx.target(), ctx.address(), ctx.userAgent());
						}
					}

					// discard possible pending request body
					try (var body = ctx.body()) {
						body.transferTo(OutputStream.nullOutputStream());
					}
				}
			}

			// close current connection
			if (HeaderValue.CLOSE.equals(ctx.headers().get(HeaderName.CONNECTION))) {
				break;
			}
		}
	}
}
