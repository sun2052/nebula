package org.byteinfo.proxy;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

public class ProxyBuilder extends ClassVisitor {
	public static final String CLASS_INITIALIZER = "<clinit>";
	public static final String INSTANCE_INITIALIZER = "<init>";
	public static final String NAME_SEPARATOR = "$$";
	public static final String NAME_POSTFIX = NAME_SEPARATOR + "Proxy";
	public static final String ADVICE_DESCRIPTOR = Type.getMethodDescriptor(Advice.class.getDeclaredMethods()[0]);
	public static final String PROXY_TARGET_TYPE = Type.getInternalName(ProxyTarget.class);

	private ClassWriter cw;
	private List<Aspect> aspects;
	private String typeName;
	private String superName;

	public ProxyBuilder(ClassWriter cw, List<Aspect> aspects) {
		super(ASM9);
		this.cw = cw;
		this.aspects = aspects;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.typeName = name + NAME_POSTFIX;
		this.superName = name;
		cw.visit(version, access, typeName, signature, name, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (name.equals(CLASS_INITIALIZER)) {
			return null;
		}

		if (name.equals(INSTANCE_INITIALIZER)) {
			MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, superName, name, descriptor, false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
			return null;
		}

		return new MethodVisitor(ASM9) {
			private List<String> annotationTypes = new ArrayList<>();

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				annotationTypes.add(descriptor);
				return null;
			}

			@Override
			public void visitEnd() {
				MethodInfo info = new MethodInfo(name, descriptor, annotationTypes.toArray(new String[0]));

				List<Class<? extends Advice>> list = new ArrayList<>(aspects.size());
				for (Aspect aspect : aspects) {
					if (aspect.pointcut.apply(info)) {
						if ((access & ACC_PRIVATE) == 0 && (access & ACC_FINAL) == 0 && (access & ACC_STATIC) == 0 && (access & ACC_ABSTRACT) == 0) {
							list.add(aspect.advice);
						} else {
							throw new IllegalArgumentException("Only non-final and non-private instance methods are allowed: " + name);
						}
					}
				}

				if (!list.isEmpty()) {
					String nextDescriptor = descriptor.substring(0, descriptor.indexOf(')') + 1) + AsmUtil.DESCRIPTOR_OBJECT;

					for (int i = 0; i < list.size(); i++) {
						boolean isFirst = i == 0;
						boolean isLast = i == list.size() - 1;
						String currentMethodName = isFirst ? name : name + NAME_SEPARATOR + i;
						String nextMethodName = isLast ? name : name + NAME_SEPARATOR + (i + 1);
						try {
							ClassReader cr = new ClassReader(list.get(i).getName());
							cr.accept(new ClassVisitor(ASM9) {
								@Override
								public MethodVisitor visitMethod(int access2, String name2, String descriptor2, String signature2, String[] exceptions2) {
									if (descriptor2.equals(ADVICE_DESCRIPTOR)) {
										String currentDescriptor = isFirst ? descriptor : nextDescriptor;
										MethodVisitor mv = cw.visitMethod(access, currentMethodName, currentDescriptor, signature, exceptions);
										return new MethodVisitor(ASM9, mv) {
											private int previousOpcode;
											private int previousOperand;

											@Override
											public void visitInsn(int opcode) {
												if (opcode == ARETURN && isFirst) {
													AsmUtil.unboxing(mv, info.returnType());
													AsmUtil.visitReturn(mv, info.returnType());
												} else {
													previousOpcode = opcode;
													super.visitInsn(opcode);
												}
											}

											@Override
											public void visitVarInsn(int opcode, int var) {
												super.visitVarInsn(opcode, var == 0 ? 0 : var + info.argumentsSize());
											}

											@Override
											public void visitIincInsn(int var, int increment) {
												super.visitIincInsn(var == 0 ? 0 : var + info.argumentsSize(), increment);
											}

											@Override
											public void visitIntInsn(int opcode, int operand) {
												previousOpcode = opcode;
												previousOperand = operand;
												super.visitIntInsn(opcode, operand);
											}

											@Override
											public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
												if (opcode == INVOKESTATIC && owner.equals(PROXY_TARGET_TYPE)) {
													switch (name) {
														case "proceed":
															mv.visitVarInsn(ALOAD, 0);
															for (int j = 0; j < info.argumentsCount(); j++) {
																AsmUtil.loadArgument(mv, info, j);
															}
															if (isLast) {
																mv.visitMethodInsn(INVOKESPECIAL, superName, info.name(), info.descriptor(), false);
																AsmUtil.boxing(mv, info.returnType());
															} else {
																mv.visitMethodInsn(INVOKEVIRTUAL, typeName, nextMethodName, nextDescriptor, false);
															}
															break;

														case "name":
															mv.visitLdcInsn(info.name());
															break;

														case "argumentsCount":
															AsmUtil.pushInt(mv, info.argumentsCount());
															break;

														case "argumentTypes":
															AsmUtil.loadArgumentClasses(mv, info);
															break;

														case "returnType":
															AsmUtil.loadClass(mv, info.returnType());
															break;

														case "argument":
															mv.visitInsn(POP);
															AsmUtil.loadArgumentObject(mv, info, AsmUtil.getArgumentIndex(previousOpcode, previousOperand));
															break;

														case "arguments":
															AsmUtil.loadArgumentObjects(mv, info);
															break;

														case "setArgument":
															mv.visitInsn(POP);
															AsmUtil.storeArgumentObject(mv, info, AsmUtil.getArgumentIndex(previousOpcode, previousOperand));
															break;

														default:
															throw new ProxyException("Unsupported ProxyTarget method: " + name);
													}
												} else {
													super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
												}
											}
										};
									}
									return null;
								}
							}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
						} catch (IOException e) {
							throw new ProxyException(e);
						}
					}
				}
			}
		};
	}
}
