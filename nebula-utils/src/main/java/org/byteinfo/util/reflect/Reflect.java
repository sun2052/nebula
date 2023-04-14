package org.byteinfo.util.reflect;

import org.byteinfo.util.function.Unchecked;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Reflect Util
 */
public interface Reflect {
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
		Map<String, Field> map = new LinkedHashMap<>();
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
	 * Gets all the instance fields in the specified class and all of its super classes.
	 *
	 * @param clazz target type
	 * @return fields map
	 */
	static Map<String, Field> getInstanceFields(Class<?> clazz) {
		return Reflect.getFields(clazz, field -> !Modifier.isStatic(field.getModifiers()));
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
		Map<String, Method> map = new LinkedHashMap<>();
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
	 * Get the accessor Method instance from the specified class or its superclasses recursively by the name.
	 *
	 * @param clazz specified class type
	 * @param name accessor name
	 * @return accessor (name() > getName() > isName()) or null
	 */
	static Method getAccessor(Class<?> clazz, String name) {
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			Map<String, Method> map = new HashMap<>();
			for (Method method : clazz.getDeclaredMethods()) {
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
		}
		return null;
	}

	/**
	 * Create a new instance with given type and value mapper.
	 *
	 * @param clazz target type
	 * @param mapper (fieldName, genericType) -> value mapper
	 * @return target instance
	 */
	static <T> T create(Class<T> clazz, BiFunction<String, Type, Object> mapper) throws ReflectiveOperationException {
		Map<String, Field> map = Reflect.getInstanceFields(clazz);
		if (clazz.isRecord()) {
			Class<?>[] params = new Class<?>[map.size()];
			Object[] args = new Object[map.size()];
			int i = 0;
			for (Field field : map.values()) {
				params[i] = field.getType();
				args[i] = getValueByType(field.getType(), mapper.apply(field.getName(), field.getGenericType()));
				i++;
			}
			return clazz.getDeclaredConstructor(params).newInstance(args);
		} else {
			T obj = clazz.getConstructor().newInstance();
			for (Field field : map.values()) {
				Object value = getValueByType(field.getType(), mapper.apply(field.getName(), field.getGenericType()));
				if (value != null) {
					field.set(obj, value);
				}
			}
			return obj;
		}
	}

	/**
	 * Create a new instance with given type and source object.
	 *
	 * @param clazz target type
	 * @param source source object
	 * @return target instance
	 */
	static <T> T create(Class<T> clazz, Object source) throws ReflectiveOperationException {
		Map<String, Field> map = Reflect.getInstanceFields(source.getClass());
		return create(clazz, Unchecked.biFunction((name, type) -> {
			Field field = map.get(name);
			if (field != null) {
				return field.get(source);
			}
			return null;
		}));
	}

	private static Object getValueByType(Class<?> type, Object value) {
		if (type == Optional.class) {
			if (value == null || value.getClass() != Optional.class) {
				return Optional.ofNullable(value);
			}
		} else {
			if (value != null && value.getClass() == Optional.class) {
				return ((Optional<?>) value).orElse(null);
			}
		}
		return value;
	}
}
