package org.byteinfo.proxy;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

interface AsmUtil {
	String BINARY_NAME_OBJECT = "java/lang/Object";
	String BINARY_NAME_CLASS = "java/lang/Class";

	String BINARY_NAME_BYTE = "java/lang/Byte";
	String BINARY_NAME_SHORT = "java/lang/Short";
	String BINARY_NAME_INTEGER = "java/lang/Integer";
	String BINARY_NAME_LONG = "java/lang/Long";
	String BINARY_NAME_CHARACTER = "java/lang/Character";
	String BINARY_NAME_FLOAT = "java/lang/Float";
	String BINARY_NAME_DOUBLE = "java/lang/Double";
	String BINARY_NAME_BOOLEAN = "java/lang/Boolean";
	String BINARY_NAME_VOID = "java/lang/Void";

	String DESCRIPTOR_OBJECT = "Ljava/lang/Object;";
	String DESCRIPTOR_CLASS = "Ljava/lang/Class;";

	static void valueOfInteger(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_INTEGER, "valueOf", "(I)Ljava/lang/Integer;", false);
	}

	static void valueOfLong(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_LONG, "valueOf", "(J)Ljava/lang/Long;", false);
	}

	static void valueOfBoolean(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_BOOLEAN, "valueOf", "(Z)Ljava/lang/Boolean;", false);
	}

	static void valueOfCharacter(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_CHARACTER, "valueOf", "(C)Ljava/lang/Character;", false);
	}

	static void valueOfByte(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_BYTE, "valueOf", "(B)Ljava/lang/Byte;", false);
	}

	static void valueOfShort(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_SHORT, "valueOf", "(S)Ljava/lang/Short;", false);
	}

	static void valueOfFloat(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_FLOAT, "valueOf", "(F)Ljava/lang/Float;", false);
	}

	static void valueOfDouble(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, BINARY_NAME_DOUBLE, "valueOf", "(D)Ljava/lang/Double;", false);
	}

	static void boxing(MethodVisitor mv, Type type) {
		switch (type.getDescriptor().charAt(0)) {
			case 'V' -> mv.visitInsn(ACONST_NULL);
			case 'I' -> valueOfInteger(mv);
			case 'J' -> valueOfLong(mv);
			case 'Z' -> valueOfBoolean(mv);
			case 'C' -> valueOfCharacter(mv);
			case 'B' -> valueOfByte(mv);
			case 'S' -> valueOfShort(mv);
			case 'F' -> valueOfFloat(mv);
			case 'D' -> valueOfDouble(mv);
		}
	}

	static void intValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_INTEGER);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_INTEGER, "intValue", "()I", false);
	}

	static void longValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_LONG);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_LONG, "longValue", "()J", false);
	}

	static void booleanValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_BOOLEAN);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_BOOLEAN, "booleanValue", "()Z", false);
	}

	static void charValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_CHARACTER);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_CHARACTER, "charValue", "()C", false);
	}

	static void byteValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_BYTE);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_BYTE, "byteValue", "()B", false);
	}

	static void shortValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_SHORT);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_SHORT, "shortValue", "()S", false);
	}

	static void floatValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_FLOAT);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_FLOAT, "floatValue", "()F", false);
	}

	static void doubleValue(MethodVisitor mv) {
		mv.visitTypeInsn(CHECKCAST, BINARY_NAME_DOUBLE);
		mv.visitMethodInsn(INVOKEVIRTUAL, BINARY_NAME_DOUBLE, "doubleValue", "()D", false);
	}

	static void unboxing(MethodVisitor mv, Type type) {
		switch (type.getDescriptor().charAt(0)) {
			case 'V' -> mv.visitInsn(POP);
			case 'I' -> intValue(mv);
			case 'J' -> longValue(mv);
			case 'Z' -> booleanValue(mv);
			case 'C' -> charValue(mv);
			case 'B' -> byteValue(mv);
			case 'S' -> shortValue(mv);
			case 'F' -> floatValue(mv);
			case 'D' -> doubleValue(mv);
		}
	}

	static void pushInt(MethodVisitor mv, int value) {
		if (value <= 5) {
			mv.visitInsn(ICONST_0 + value);
		} else if (value <= Byte.MAX_VALUE) {
			mv.visitIntInsn(BIPUSH, value);
		} else {
			mv.visitIntInsn(SIPUSH, value);
		}
	}

	static void checkIndex(int index, int fieldLength) {
		if (index < 0 || index >= fieldLength) {
			throw new IllegalArgumentException(String.format("index: %d (expected: 0 <= index < %d)", index, fieldLength));
		}
	}

	static void loadClass(MethodVisitor mv, Type type) {
		switch (type.getDescriptor().charAt(0)) {
			case 'V' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_VOID, "TYPE", DESCRIPTOR_CLASS);
			case 'I' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_INTEGER, "TYPE", DESCRIPTOR_CLASS);
			case 'J' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_LONG, "TYPE", DESCRIPTOR_CLASS);
			case 'Z' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_BOOLEAN, "TYPE", DESCRIPTOR_CLASS);
			case 'C' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_CHARACTER, "TYPE", DESCRIPTOR_CLASS);
			case 'B' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_BYTE, "TYPE", DESCRIPTOR_CLASS);
			case 'S' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_SHORT, "TYPE", DESCRIPTOR_CLASS);
			case 'F' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_FLOAT, "TYPE", DESCRIPTOR_CLASS);
			case 'D' -> mv.visitFieldInsn(GETSTATIC, BINARY_NAME_DOUBLE, "TYPE", DESCRIPTOR_CLASS);
			case '[', 'L' -> mv.visitLdcInsn(type);
		}
	}

	static void loadArgumentClasses(MethodVisitor mv, MethodInfo info) {
		int argsCount = info.argumentsCount();
		pushInt(mv, argsCount);
		mv.visitTypeInsn(ANEWARRAY, BINARY_NAME_CLASS);
		for (int i = 0; i < argsCount; i++) {
			mv.visitInsn(DUP);
			pushInt(mv, i);
			loadClass(mv, info.argumentTypes()[i]);
			mv.visitInsn(AASTORE);
		}
	}

	static void loadArgumentObject(MethodVisitor mv, MethodInfo info, int index) {
		checkIndex(index, info.argumentsCount());
		int offset = info.argumentOffset(index);
		Type type = info.argumentTypes()[index];
		switch (type.getDescriptor().charAt(0)) {
			case 'V' -> {}
			case 'I' -> {
				mv.visitVarInsn(ILOAD, offset);
				valueOfInteger(mv);
			}
			case 'J' -> {
				mv.visitVarInsn(LLOAD, offset);
				valueOfLong(mv);
			}
			case 'Z' -> {
				mv.visitVarInsn(ILOAD, offset);
				valueOfBoolean(mv);
			}
			case 'C' -> {
				mv.visitVarInsn(ILOAD, offset);
				valueOfCharacter(mv);
			}
			case 'B' -> {
				mv.visitVarInsn(ILOAD, offset);
				valueOfByte(mv);
			}
			case 'S' -> {
				mv.visitVarInsn(ILOAD, offset);
				valueOfShort(mv);
			}
			case 'F' -> {
				mv.visitVarInsn(FLOAD, offset);
				valueOfFloat(mv);
			}
			case 'D' -> {
				mv.visitVarInsn(DLOAD, offset);
				valueOfDouble(mv);
			}
			case '[', 'L' -> mv.visitVarInsn(ALOAD, offset);
		}
	}

	static void loadArgumentObjects(MethodVisitor mv, MethodInfo info) {
		int argsCount = info.argumentsCount();
		pushInt(mv, argsCount);
		mv.visitTypeInsn(ANEWARRAY, BINARY_NAME_OBJECT);
		for (int i = 0; i < argsCount; i++) {
			mv.visitInsn(DUP);
			pushInt(mv, i);
			loadArgumentObject(mv, info, i);
			mv.visitInsn(AASTORE);
		}
	}

	static int getArgumentIndex(int opcode, int operand) {
		return switch (opcode) {
			case ICONST_0 -> 0;
			case ICONST_1 -> 1;
			case ICONST_2 -> 2;
			case ICONST_3 -> 3;
			case ICONST_4 -> 4;
			case ICONST_5 -> 5;
			case BIPUSH, SIPUSH -> operand;
			default -> throw new ProxyException("Unexpected previous instruction used for setting argument index.");
		};
	}

	static void storeArgumentObject(MethodVisitor mv, MethodInfo info, int index) {
		checkIndex(index, info.argumentsCount());
		Type type = info.argumentTypes()[index];
		int offset = info.argumentOffset(index);
		storeValue(mv, type, offset);
	}

	static void storeValue(MethodVisitor mv, Type type, int offset) {
		switch (type.getDescriptor().charAt(0)) {
			case 'V' -> {}
			case 'I' -> {
				intValue(mv);
				mv.visitVarInsn(ISTORE, offset);
			}
			case 'J' -> {
				longValue(mv);
				mv.visitVarInsn(LSTORE, offset);
			}
			case 'Z' -> {
				booleanValue(mv);
				mv.visitVarInsn(ISTORE, offset);
			}
			case 'C' -> {
				charValue(mv);
				mv.visitVarInsn(ISTORE, offset);
			}
			case 'B' -> {
				byteValue(mv);
				mv.visitVarInsn(ISTORE, offset);
			}
			case 'S' -> {
				shortValue(mv);
				mv.visitVarInsn(ISTORE, offset);
			}
			case 'F' -> {
				floatValue(mv);
				mv.visitVarInsn(FSTORE, offset);
			}
			case 'D' -> {
				doubleValue(mv);
				mv.visitVarInsn(DSTORE, offset);
			}
			case '[', 'L' -> mv.visitVarInsn(ASTORE, offset);
		}
	}

	static void loadArgument(MethodVisitor mv, MethodInfo info, int index) {
		Type type = info.argumentTypes()[index];
		int offset = info.argumentOffset(index);
		switch (type.getDescriptor().charAt(0)) {
			case 'V' -> {}
			case 'I', 'Z', 'C', 'B', 'S' -> mv.visitVarInsn(ILOAD, offset);
			case 'J' -> mv.visitVarInsn(LLOAD, offset);
			case 'F' -> mv.visitVarInsn(FLOAD, offset);
			case 'D' -> mv.visitVarInsn(DLOAD, offset);
			case '[', 'L' -> mv.visitVarInsn(ALOAD, offset);
		}
	}

	static void visitReturn(MethodVisitor mv, Type type) {
		switch (type.getDescriptor().charAt(0)) {
			case 'V' -> mv.visitInsn(RETURN);
			case 'I', 'Z', 'C', 'B', 'S' -> mv.visitInsn(IRETURN);
			case 'J' -> mv.visitInsn(LRETURN);
			case 'F' -> mv.visitInsn(FRETURN);
			case 'D' -> mv.visitInsn(DRETURN);
			case '[', 'L' -> {
				mv.visitTypeInsn(CHECKCAST, type.getInternalName());
				mv.visitInsn(ARETURN);
			}
		}
	}
}
