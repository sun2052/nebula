package org.byteinfo.proxy;

import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;

public class MethodInfo {
	private final String name;
	private final String descriptor;
	private final String[] annotationDescriptors;
	private final Type[] argumentTypes;
	private final Type returnType;
	private final int[] argumentOffsets;
	private final int argumentsSize;

	MethodInfo(String name, String descriptor, String[] annotationDescriptors) {
		this.name = name;
		this.descriptor = descriptor;
		this.annotationDescriptors = annotationDescriptors;
		this.argumentTypes = Type.getArgumentTypes(descriptor);
		this.returnType = Type.getReturnType(descriptor);
		argumentOffsets = new int[argumentTypes.length];
		int size = 1;
		for (int i = 0; i < argumentTypes.length; i++) {
			argumentOffsets[i] = size;
			size += argumentTypes[i].getSize();
		}
		argumentsSize = size;
	}

	public String name() {
		return name;
	}

	public String descriptor() {
		return descriptor;
	}

	public String[] annotationDescriptors() {
		return annotationDescriptors;
	}

	public boolean hasAnnotation(Class<? extends Annotation> clazz) {
		if (annotationDescriptors.length == 0) {
			return false;
		}
		String target = Type.getDescriptor(clazz);
		for (String annotation : annotationDescriptors) {
			if (annotation.equals(target)) {
				return true;
			}
		}
		return false;
	}

	public int argumentsCount() {
		return argumentTypes.length;
	}

	public Type[] argumentTypes() {
		return argumentTypes;
	}

	public Type returnType() {
		return returnType;
	}

	public int argumentOffset(int index) {
		return argumentOffsets[index];
	}

	public int argumentsSize() {
		return argumentsSize;
	}
}
