package org.byteinfo.proxy;

import org.objectweb.asm.Type;

import java.util.Set;

public class MethodInfo {
	private final String name;
	private final String descriptor;
	private final Set<String> annotationTypes;
	private final Type[] argumentTypes;
	private final Type returnType;
	private final int[] argumentOffsets;

	MethodInfo(String name, String descriptor, Set<String> annotationTypes) {
		this.name = name;
		this.descriptor = descriptor;
		this.annotationTypes = annotationTypes;
		this.argumentTypes = Type.getArgumentTypes(descriptor);
		this.returnType = Type.getReturnType(descriptor);
		argumentOffsets = new int[argumentTypes.length];
		int offset = 0;
		for (int i = 0; i < argumentTypes.length; i++) {
			offset += argumentTypes[i].getSize();
			argumentOffsets[i] = offset;
		}
	}

	public String name() {
		return name;
	}

	public String descriptor() {
		return descriptor;
	}

	public Set<String> annotationTypes() {
		return annotationTypes;
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
}
