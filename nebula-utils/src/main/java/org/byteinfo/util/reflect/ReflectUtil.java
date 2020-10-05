package org.byteinfo.util.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * ReflectUtil
 */
public interface ReflectUtil {

	/**
	 * Visits all the fields in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @param visitor field visitor or null, return false to abort
	 * @return fields map
	 */
	static Map<String, Field> visitField(Class<?> clazz, Function<Field, Boolean> visitor) {
		Map<String, Field> map = new HashMap<>();
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				field.setAccessible(true);
				Field previous = map.putIfAbsent(field.getName(), field);
				if (previous == null && visitor != null) {
					if (!visitor.apply(field)) {
						return map;
					}
				}
			}
		}
		return map;
	}

	/**
	 * Visits all the methods in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @param visitor method visitor or null, return false to abort
	 * @return methods map
	 */
	static Map<String, Method> visitMethod(Class<?> clazz, Function<Method, Boolean> visitor) {
		Map<String, Method> map = new HashMap<>();
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				method.setAccessible(true);
				Method previous = map.putIfAbsent(method.getName(), method);
				if (previous == null && visitor != null) {
					if (!visitor.apply(method)) {
						return map;
					}
				}
			}
		}
		return map;
	}

	/**
	 * Gets all the fields in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @return fields map
	 */
	static Map<String, Field> getAllFields(Class<?> clazz) {
		return visitField(clazz, null);
	}

	/**
	 * Gets all the methods in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @return fields map
	 */
	static Map<String, Method> getAllMethods(Class<?> clazz) {
		return visitMethod(clazz, null);
	}

	/**
	 * Bypass access control of the specified object.
	 *
	 * @param object target object
	 * @return target object
	 */
	static <T extends AccessibleObject> T makeAccessible(T object) {
		object.setAccessible(true);
		return object;
	}

	/**
	 * Get the Field instance from the specified class or its superclasses recursively by the name.
	 *
	 * @param clazz specified class type
	 * @param name field name
	 * @return target field or null
	 */
	static Field getField(Class<?> clazz, String name) {
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return makeAccessible(field);
				}
			}
		}
		return null;
	}

	/**
	 * Get the Method instance from the specified class or its superclasses recursively by the name.
	 *
	 * @param clazz specified class type
	 * @param name method name
	 * @return target method or null
	 */
	static Method getMethod(Class<?> clazz, String name) {
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				if (method.getName().equals(name)) {
					return makeAccessible(method);
				}
			}
		}
		return null;
	}

	/**
	 * Get the accessor Method instance from the specified class by the name.
	 *
	 * @param clazz specified class type
	 * @param name accessor name
	 * @return accessor (name > getName > isName) or null
	 */
	static Method getAccessorOn(Class<?> clazz, String name) {
		Method[] methods = clazz.getDeclaredMethods();
		Map<String, Method> map = new HashMap<>(methods.length);

		for (Method method : methods) {
			if (!method.getReturnType().equals(void.class)) {
				map.put(method.getName(), method);
			}
		}

		Method method = map.get(name);
		if (method != null) {
			return makeAccessible(method);
		}

		String upperName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		method = map.get("get" + upperName);
		if (method != null) {
			return makeAccessible(method);
		}

		method = map.get("is" + upperName);
		if (method != null && (method.getReturnType().equals(boolean.class) || method.getReturnType().equals(Boolean.class))) {
			return makeAccessible(method);
		}

		return null;
	}

	/**
	 * Get the accessor Method instance from the specified class or its superclasses recursively by the name.
	 *
	 * @param clazz specified class type
	 * @param name accessor name
	 * @return accessor (name > getName > isName) or null
	 */
	static Method getAccessor(Class<?> clazz, String name) {
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			Method method = getAccessorOn(current, name);
			if (method != null) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Get the default interface accessor Method instance from the specified class or its superclasses recursively by the name.
	 *
	 * @param clazz specified interface class type
	 * @param name method name
	 * @return target method or null
	 */
	static Method getDefaultAccessor(Class<?> clazz, String name) {
		// enumerate the transitive closure of all interfaces implemented by clazz
		Set<Class<?>> ifaces = new LinkedHashSet<>();
		for (Class<?> cc = clazz; cc != null && cc != Object.class; cc = cc.getSuperclass()) {
			addIfaces(ifaces, cc, false);
		}

		// now search those in the order that we found them
		for (Class<?> iface : ifaces) {
			Method method = getAccessorOn(iface, name);
			if (method != null) {
				return method;
			}
		}

		return null;
	}

	private static void addIfaces(Set<Class<?>> ifaces, Class<?> clazz, boolean isIface) {
		if (isIface) {
			ifaces.add(clazz);
		}

		for (Class<?> iface : clazz.getInterfaces()) {
			addIfaces(ifaces, iface, true);
		}
	}
}
