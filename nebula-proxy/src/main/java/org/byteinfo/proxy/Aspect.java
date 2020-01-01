package org.byteinfo.proxy;

public class Aspect {
	public final Pointcut pointcut;
	public final Class<? extends Advice> advice;

	private Aspect(Pointcut pointcut, Class<? extends Advice> advice) {
		this.pointcut = pointcut;
		this.advice = advice;
	}

	public static Aspect of(Pointcut pointcut, Class<? extends Advice> advice) {
		return new Aspect(pointcut, advice);
	}
}
