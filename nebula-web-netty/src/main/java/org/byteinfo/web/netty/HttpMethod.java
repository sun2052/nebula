package org.byteinfo.web.netty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpMethod {
	String GET = "GET";
	String POST = "POST";
	String PUT = "PUT";
	String DELETE = "DELETE";
	String HEAD = "HEAD";
	String OPTIONS = "OPTIONS";
	String TRACE = "TRACE";

	String value();
}
