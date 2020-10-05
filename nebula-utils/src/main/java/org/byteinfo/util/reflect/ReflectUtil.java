package org.byteinfo.util.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * ReflectUtil
 */
public interface ReflectUtil {

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
					field.setAccessible(true);
					return field;
				}
			}
		}
		return null;
	}

	/**
	 * Gets all the fields in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @param predicate determine if it should be included
	 * @return fields map
	 */
	static Map<String, Field> getFields(Class<?> clazz, Predicate<Field> predicate) {
		Map<String, Field> map = new HashMap<>();
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (predicate.test(field)) {
					Field previous = map.putIfAbsent(field.getName(), field);
					if (previous == null) {
						field.setAccessible(true);
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
		return getFields(clazz, field -> true);
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
					method.setAccessible(true);
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Visits all the methods in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @param predicate determine if it should be included
	 * @return methods map
	 */
	static Map<String, Method> getMethods(Class<?> clazz, Predicate<Method> predicate) {
		Map<String, Method> map = new HashMap<>();
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				if (predicate.test(method)) {
					Method previous = map.putIfAbsent(method.getName(), method);
					if (previous == null) {
						method.setAccessible(true);
					}
				}
			}
		}
		return map;
	}

	/**
	 * Gets all the methods in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @return fields map
	 */
	static Map<String, Method> getAllMethods(Class<?> clazz) {
		return getMethods(clazz, method -> true);
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
			method.setAccessible(true);
			return method;
		}

		String upperName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		method = map.get("get" + upperName);
		if (method != null) {
			method.setAccessible(true);
			return method;
		}

		method = map.get("is" + upperName);
		if (method != null && (method.getReturnType().equals(boolean.class) || method.getReturnType().equals(Boolean.class))) {
			method.setAccessible(true);
			return method;
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
}
