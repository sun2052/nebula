package org.byteinfo.proxy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Proxy<T> {
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	private Class<T> clazz;
	private List<Aspect> aspects = new ArrayList<>();
	private Path debugPath;

	private Proxy(Class<T> clazz) {
		this.clazz = clazz;
	}

	public static <T> Proxy<T> of(Class<T> clazz) {
		return new Proxy<>(clazz);
	}

	public Proxy<T> with(Aspect... aspects) {
		for (Aspect aspect : aspects) {
			Class<?> advice = aspect.advice();
			if (advice.getDeclaredFields().length > 0) {
				throw new IllegalArgumentException("Fields in advice is not supported.");
			}
			if (advice.getDeclaredMethods().length > 1) {
				throw new IllegalArgumentException("Exactly one method is required in advice.");
			}
		}
		this.aspects.addAll(Arrays.asList(aspects));
		return this;
	}

	public Proxy<T> debugPath(Path debugPath) {
		this.debugPath = debugPath;
		return this;
	}

	public byte[] create() throws IOException {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		ClassReader cr = new ClassReader(clazz.getName());
		cr.accept(new ProxyBuilder(cw, aspects), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		byte[] proxyClass = cw.toByteArray();
		if (debugPath != null) {
			Files.write(debugPath.resolve(clazz.getSimpleName() + ProxyBuilder.NAME_POSTFIX + ".class"), proxyClass);
		}
		return proxyClass;
	}

	@SuppressWarnings("unchecked")
	public Class<T> load() {
		try {
			return (Class<T>) MethodHandles.privateLookupIn(clazz, LOOKUP).defineClass(create());
		} catch (Exception e) {
			throw new ProxyException(e);
		}
	}

	public T instance() {
		try {
			Class<T> proxyClass = load();
			Constructor<T> constructor = proxyClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (NoSuchMethodException e) {
			throw new ProxyException("Default constructor is required for proxying " + clazz);
		} catch (Exception e) {
			throw new ProxyException(e);
		}
	}
}
