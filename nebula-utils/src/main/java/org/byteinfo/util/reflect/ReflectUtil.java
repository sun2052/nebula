package org.byteinfo.util.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * ReflectUtil
 */
public class ReflectUtil {
	/**
	 * Bypass access control of the specified object.
	 *
	 * @param object target object
	 * @return target object
	 */
	public static <T extends AccessibleObject> T makeAccessible(T object) {
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
	public static Field getField(Class<?> clazz, String name) {
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
	public static Method getMethod(Class<?> clazz, String name) {
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
	public static Method getAccessorOn(Class<?> clazz, String name) {
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
	public static Method getAccessor(Class<?> clazz, String name) {
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
	public static Method getDefaultAccessor(Class<?> clazz, String name) {
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
