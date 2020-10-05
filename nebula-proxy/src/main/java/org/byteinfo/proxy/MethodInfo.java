package org.byteinfo.proxy;

import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;

public record MethodInfo(String name, String descriptor, String[] annotationDescriptors, Type[] argumentTypes, Type returnType, int[] argumentOffsets, int argumentsSize) {

	public static MethodInfo of(String name, String descriptor, String[] annotationDescriptors) {
		Type[] argumentTypes = Type.getArgumentTypes(descriptor);
		int[] argumentOffsets = new int[argumentTypes.length];
		int argumentsSize = 1;
		for (int i = 0; i < argumentTypes.length; i++) {
			argumentOffsets[i] = argumentsSize;
			argumentsSize += argumentTypes[i].getSize();
		}
		return new MethodInfo(name, descriptor, annotationDescriptors, argumentTypes, Type.getReturnType(descriptor), argumentOffsets, argumentsSize);
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

	public int argumentOffset(int index) {
		return argumentOffsets[index];
	}
}
