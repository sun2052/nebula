package org.byteinfo.util.bean;

import org.byteinfo.util.reflect.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public interface BeanUtil {

	/**
	 * Copy the property values of the given source bean into the target bean.
	 *
	 * @param source source bean
	 * @param target target bean
	 */
	static void copy(Object source, Object target) throws IllegalAccessException {
		Map<String, Field> sourceMap = ReflectUtil.getAllFields(source.getClass());
		Map<String, Field> targetMap = ReflectUtil.getAllFields(target.getClass());
		for (Field field : targetMap.values()) {
			Field src = sourceMap.get(field.getName());
			if (src != null) {
				Object value = src.get(source);
				if (value instanceof Optional) {
					value = ((Optional<?>) value).orElse(null);
				}
				field.set(target, value);
			}
		}
	}

	/**
	 * Copy the property values of the given source bean into the target bean.
	 *
	 * @param source source bean
	 * @param target target bean type
	 * @return target bean
	 */
	static <T> T copy(Object source, Class<T> target) throws ReflectiveOperationException {
		Object obj = target.getConstructor().newInstance();
		copy(source, obj);
		return target.cast(obj);
	}
}
