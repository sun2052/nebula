package org.byteinfo.context;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * A key used for identifying specific dependency.
 */
public class Key<T> {
	public final Class<T> type;
	public final Class<? extends Annotation> qualifier;
	public final String name;
	private final int hash;

	private Key(Class<T> type, Class<? extends Annotation> qualifier, String name) {
		this.type = type;
		this.qualifier = qualifier;
		this.name = name;
		hash = Objects.hash(type, qualifier, name);
	}

	/**
	 * Gets the key for a given type.
	 *
	 * @param type target type
	 * @return key
	 */
	public static <T> Key<T> of(Class<T> type) {
		return new Key<>(type, null, null);
	}

	/**
	 * Gets the key for a given type and qualifier annotation type.
	 *
	 * @param type target type
	 * @param qualifier qualifier type
	 * @return key
	 */
	public static <T> Key<T> of(Class<T> type, Class<? extends Annotation> qualifier) {
		return new Key<>(type, qualifier, null);
	}

	/**
	 * Gets the key for a given type and name (@Named value).
	 *
	 * @param type target type
	 * @param name qualifier name
	 * @return key
	 */
	public static <T> Key<T> of(Class<T> type, String name) {
		return new Key<>(type, Named.class, name);
	}

	/**
	 * Gets the key for a given type and qualifier annotation.
	 *
	 * @param type target type
	 * @param qualifier qualifier annotation
	 * @return key
	 */
	public static <T> Key<T> of(Class<T> type, Annotation qualifier) {
		if (qualifier == null) {
			return Key.of(type);
		} else {
			return qualifier.annotationType().equals(Named.class) ? Key.of(type, ((Named) qualifier).value()) : Key.of(type, qualifier.annotationType());
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Key<?> key = (Key<?>) o;
		return Objects.equals(type, key.type) && Objects.equals(qualifier, key.qualifier) && Objects.equals(name, key.name);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		String str = type.getName();
		if (name != null) {
			str += "@\"" + name + "\"";
		} else if (qualifier != null) {
			str += "@" + qualifier.getSimpleName();
		}
		return str;
	}
}
