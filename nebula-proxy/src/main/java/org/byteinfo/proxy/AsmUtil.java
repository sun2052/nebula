package org.byteinfo.proxy;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;

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
			case 'V':
				mv.visitInsn(ACONST_NULL);
				break;

			case 'I':
				valueOfInteger(mv);
				break;

			case 'J':
				valueOfLong(mv);
				break;

			case 'Z':
				valueOfBoolean(mv);
				break;

			case 'C':
				valueOfCharacter(mv);
				break;

			case 'B':
				valueOfByte(mv);
				break;

			case 'S':
				valueOfShort(mv);
				break;

			case 'F':
				valueOfFloat(mv);
				break;

			case 'D':
				valueOfDouble(mv);
				break;

			default:
				// ignore
				break;
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
			case 'V':
				mv.visitInsn(POP);
				break;

			case 'I':
				intValue(mv);
				break;

			case 'J':
				longValue(mv);
				break;

			case 'Z':
				booleanValue(mv);
				break;

			case 'C':
				charValue(mv);
				break;

			case 'B':
				byteValue(mv);
				break;

			case 'S':
				shortValue(mv);
				break;

			case 'F':
				floatValue(mv);
				break;

			case 'D':
				doubleValue(mv);
				break;

			default:
				// ignore
				break;
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
			case 'V':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_VOID, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'I':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_INTEGER, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'J':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_LONG, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'Z':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_BOOLEAN, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'C':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_CHARACTER, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'B':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_BYTE, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'S':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_SHORT, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'F':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_FLOAT, "TYPE", DESCRIPTOR_CLASS);
				break;

			case 'D':
				mv.visitFieldInsn(GETSTATIC, BINARY_NAME_DOUBLE, "TYPE", DESCRIPTOR_CLASS);
				break;

			default:
				mv.visitLdcInsn(type);
				break;
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
			case 'V':
				break;

			case 'I':
				mv.visitVarInsn(ILOAD, offset);
				valueOfInteger(mv);
				break;

			case 'J':
				mv.visitVarInsn(LLOAD, offset);
				valueOfLong(mv);
				break;

			case 'Z':
				mv.visitVarInsn(ILOAD, offset);
				valueOfBoolean(mv);
				break;

			case 'C':
				mv.visitVarInsn(ILOAD, offset);
				valueOfCharacter(mv);
				break;

			case 'B':
				mv.visitVarInsn(ILOAD, offset);
				valueOfByte(mv);
				break;

			case 'S':
				mv.visitVarInsn(ILOAD, offset);
				valueOfShort(mv);
				break;

			case 'F':
				mv.visitVarInsn(FLOAD, offset);
				valueOfFloat(mv);
				break;

			case 'D':
				mv.visitVarInsn(DLOAD, offset);
				valueOfDouble(mv);
				break;

			default:
				mv.visitVarInsn(ALOAD, offset);
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
		switch (opcode) {
			case ICONST_0:
				return 0;

			case ICONST_1:
				return 1;

			case ICONST_2:
				return 2;

			case ICONST_3:
				return 3;

			case ICONST_4:
				return 4;

			case ICONST_5:
				return 5;

			case BIPUSH:
			case SIPUSH:
				return operand;

			default:
				throw new ProxyException("Unexpected previous instruction used for setting argument index.");
		}
	}

	static void storeArgumentObject(MethodVisitor mv, MethodInfo info, int index) {
		checkIndex(index, info.argumentsCount());
		Type type = info.argumentTypes()[index];
		int offset = info.argumentOffset(index);
		storeValue(mv, type, offset);
	}

	static void storeValue(MethodVisitor mv, Type type, int offset) {
		switch (type.getDescriptor().charAt(0)) {
			case 'V':
				break;

			case 'I':
				intValue(mv);
				mv.visitVarInsn(ISTORE, offset);
				break;

			case 'J':
				longValue(mv);
				mv.visitVarInsn(LSTORE, offset);
				break;

			case 'Z':
				booleanValue(mv);
				mv.visitVarInsn(ISTORE, offset);
				break;

			case 'C':
				charValue(mv);
				mv.visitVarInsn(ISTORE, offset);
				break;

			case 'B':
				byteValue(mv);
				mv.visitVarInsn(ISTORE, offset);
				break;

			case 'S':
				shortValue(mv);
				mv.visitVarInsn(ISTORE, offset);
				break;

			case 'F':
				floatValue(mv);
				mv.visitVarInsn(FSTORE, offset);
				break;

			case 'D':
				doubleValue(mv);
				mv.visitVarInsn(DSTORE, offset);
				break;

			default:
				mv.visitVarInsn(ASTORE, offset);
		}
	}

	static void loadArgument(MethodVisitor mv, MethodInfo info, int index) {
		Type type = info.argumentTypes()[index];
		int offset = info.argumentOffset(index);
		switch (type.getDescriptor().charAt(0)) {
			case 'V':
				break;

			case 'I':
			case 'Z':
			case 'C':
			case 'B':
			case 'S':
				mv.visitVarInsn(ILOAD, offset);
				break;

			case 'J':
				mv.visitVarInsn(LLOAD, offset);
				break;

			case 'F':
				mv.visitVarInsn(FLOAD, offset);
				break;

			case 'D':
				mv.visitVarInsn(DLOAD, offset);
				break;

			default:
				mv.visitVarInsn(ALOAD, offset);
		}
	}

	static void visitReturn(MethodVisitor mv, Type type) {
		switch (type.getDescriptor().charAt(0)) {
			case 'V':
				mv.visitInsn(RETURN);
				break;

			case 'I':
			case 'Z':
			case 'C':
			case 'B':
			case 'S':
				mv.visitInsn(IRETURN);
				break;

			case 'J':
				mv.visitInsn(LRETURN);
				break;

			case 'F':
				mv.visitInsn(FRETURN);
				break;

			case 'D':
				mv.visitInsn(DRETURN);
				break;

			default:
				mv.visitTypeInsn(CHECKCAST, type.getInternalName());
				mv.visitInsn(ARETURN);
				break;
		}
	}
}
