package org.byteinfo.proxy;

public record Aspect(Pointcut pointcut, Class<? extends Advice> advice) {
	public static Aspect of(Pointcut pointcut, Class<? extends Advice> advice) {
		return new Aspect(pointcut, advice);
	}
}
