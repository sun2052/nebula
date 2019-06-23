package org.byteinfo.context;

import javax.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a type that the injector instantiates a new instance each time. Not inherited.
 *
 * @see javax.inject.Scope @Scope
 */
@Scope
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Prototype {
}
