package org.byteinfo.web;

import org.byteinfo.context.Context;
import org.byteinfo.logging.Log;
import org.byteinfo.util.function.CheckedConsumer;
import org.byteinfo.util.misc.Config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Context {
	private final ExecutorService executor = Executors.newVirtualThreadExecutor();

	private List<CheckedConsumer<Server>> onStartHandlers = new ArrayList<>();
	private List<CheckedConsumer<Server>> onStopHandlers = new ArrayList<>();

	// HTTP Handlers: path -> (method -> handler)
	private final Map<String, Map<String, Handler>> exactHandlers = new HashMap<>();
	private final Map<String, Map<String, Handler>> genericHandlers = new LinkedHashMap<>();

	// HTTP Filters
	private final List<Filter> filters = new ArrayList<>();

	// HTTP Security Attributes
	Map<Handler, String> securityAttributes = new HashMap<>();

	// Error Handler
	private ErrorHandler errorHandler = (ctx, t) -> {
		if (t instanceof WebException e) {
			t = e.getCause();
			ctx.setResponseStatus(e.getStatus());
		} else {
			ctx.setResponseStatus(StatusCode.INTERNAL_SERVER_ERROR);
		}
		if (t != null) {
			Log.error(t, "Error encountered while handling: {} {}", ctx.method(), ctx.path());
		}
		return "ERROR: " + ctx.responseStatus();
	};

	// HTTP Asset Handler
	private final Handler ASSET_HANDLER;

	private volatile ServerSocket serverSocket;
	private volatile boolean started;

	public Server(Object... modules) throws IOException {
		super(modules);

		// load configurations
		Config.load("class:org/byteinfo/web/application.properties");
		Config.loadIf("class:application.properties");

		// init runtime variables
		int processors = Runtime.getRuntime().availableProcessors();
		Config.load(Map.of(
				"runtime.pid", ProcessHandle.current().pid(),
				"runtime.processors", processors));

		// system properties have the highest priority
		Config.load(System.getProperties());

		// interpolate variables
		Config.interpolate();

		// init asset handler
		ASSET_HANDLER = new AssetHandler();
	}

	public Server start() throws Exception {
		if (started) {
			return this;
		}
		int port = Config.getInt("http.port");
		int backlog = Config.getInt("http.backlog");
		InetAddress bindAddr = InetAddress.getByName(Config.get("http.bindAddr"));
		int timeout = Config.getInt("session.timeout") * 60 * 1000;
		serverSocket = new ServerSocket(port, backlog, bindAddr);
		serverSocket.setReuseAddress(true);
		Thread main = new Thread(() -> {
			while (started) {
				try {
					Socket socket = serverSocket.accept();
					executor.execute(() -> {
						try (socket) {
							Log.debug("Connected: {}", socket);
							socket.setTcpNoDelay(true);
							socket.setSoTimeout(timeout);
							handleConnection(socket);
						} catch (SocketTimeoutException e) {
							// ignored
						} catch (Throwable t) {
							Log.error(t);
						} finally {
							Log.debug("Disconnected: {}", socket);
						}
					});
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			}
		});
		main.setName(getClass().getSimpleName() + "-" + port);
		main.start();
		started = true;
		for (CheckedConsumer<Server> handler : onStartHandlers) {
			handler.accept(this);
		}
		Log.info("Server started: listening {}", port);
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
		return this;
	}

	public Server onStart(CheckedConsumer<Server> handler) {
		onStartHandlers.add(handler);
		return this;
	}

	public Server onStop(CheckedConsumer<Server> handler) {
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
		filters.add(filter);
		return this;
	}

	public Server filter(Class<? extends Filter>... classes) {
		for (Class<? extends Filter> clazz : classes) {
			filters.add(instance(clazz));
		}
		return this;
	}

	public Server error(ErrorHandler handler) {
		this.errorHandler = handler;
		return this;
	}

	public Server error(Class<? extends ErrorHandler> clazz) {
		errorHandler = instance(clazz);
		return this;
	}

	private void handleConnection(Socket socket) throws Exception {
		InputStream in = new BufferedInputStream(socket.getInputStream());
		OutputStream out = new BufferedOutputStream(socket.getOutputStream());
		OutputStream nullOut = OutputStream.nullOutputStream();
		while (true) {
			HttpContext ctx = null;
			Object result = null;
			Handler handler = null;
			Throwable th = null;
			try {
				// parse request
				ctx = new HttpContext(socket, in, out);

				// exact handler
				handler = exactHandlers.getOrDefault(ctx.path(), Collections.emptyMap()).get(ctx.method());

				// generic handler
				if (handler == null) {
					for (Map.Entry<String, Map<String, Handler>> entry : genericHandlers.entrySet()) {
						if (ctx.path().startsWith(entry.getKey())) {
							handler = entry.getValue().get(ctx.method());
							break;
						}
					}
				}

				// set security attribute
				if (handler != null) {
					ctx.setSecurityAttribute(securityAttributes.get(handler));
				}

				// asset handler
				if (handler == null) {
					handler = ASSET_HANDLER;
				}

				// before
				for (Filter filter : filters) {
					filter.before(ctx, handler);
					if (ctx.isCommitted()) {
						break;
					}
				}

				// handler
				if (!ctx.isCommitted()) {
					result = handler.handle(ctx);

					// after
					for (int i = filters.size() - 1; i >= 0; i--) {
						filters.get(i).after(ctx, handler, result);
						if (ctx.isCommitted()) {
							break;
						}
					}
				}
			} catch (Throwable t) {
				th = t;
				if (ctx == null) { // failed to parse request
					HttpCodec.writeResponse(out, StatusCode.BAD_REQUEST);
					throw t;
				} else {
					result = errorHandler.handle(ctx, t);
				}
			} finally {
				if (ctx != null) {
					// send response
					if (!ctx.isCommitted()) {
						ctx.commit(result);
					}

					// complete
					for (Filter filter : filters) {
						try {
							filter.complete(ctx, handler, th);
						} catch (Exception e) {
							Log.error(e, "Filter failed: {}", ctx.path());
						}
					}

					// clear pending request body
					ctx.body().transferTo(nullOut);
				}
			}

			// disconnect
			if ("close".equalsIgnoreCase(ctx.headers().get(HeaderName.CONNECTION))) {
				break;
			}
		}
	}
}
