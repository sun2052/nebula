package org.byteinfo.proxy;

import java.io.IOException;
import java.lang.classfile.AccessFlags;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.NopInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Proxy<T> {
	private static final String NAME_SEPARATOR = "$$";
	private static final String NAME_POSTFIX = NAME_SEPARATOR + "PROXY";
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	private static final Map<ClassDesc, ClassDesc> BOX_MAP;
	private static final Map<ClassDesc, ClassDesc> UNBOX_MAP;

	private Class<T> targetClass;
	private ClassDesc proxyClassType;
	private List<Aspect> aspects = new ArrayList<>();
	private Path debugPath;

	static {
		BOX_MAP = Map.of(
				ConstantDescs.CD_int, ConstantDescs.CD_Integer,
				ConstantDescs.CD_long, ConstantDescs.CD_Long,
				ConstantDescs.CD_float, ConstantDescs.CD_Float,
				ConstantDescs.CD_double, ConstantDescs.CD_Double,
				ConstantDescs.CD_short, ConstantDescs.CD_Short,
				ConstantDescs.CD_byte, ConstantDescs.CD_Byte,
				ConstantDescs.CD_char, ConstantDescs.CD_Character,
				ConstantDescs.CD_boolean, ConstantDescs.CD_Boolean
		);
		UNBOX_MAP = Map.of(
				ConstantDescs.CD_Integer, ConstantDescs.CD_int,
				ConstantDescs.CD_Long, ConstantDescs.CD_long,
				ConstantDescs.CD_Float, ConstantDescs.CD_float,
				ConstantDescs.CD_Double, ConstantDescs.CD_double,
				ConstantDescs.CD_Short, ConstantDescs.CD_short,
				ConstantDescs.CD_Byte, ConstantDescs.CD_byte,
				ConstantDescs.CD_Character, ConstantDescs.CD_char,
				ConstantDescs.CD_Boolean, ConstantDescs.CD_boolean
		);
	}

	private Proxy(Class<T> targetClass) {
		this.targetClass = targetClass;
		this.proxyClassType = ClassDesc.of(targetClass.getName() + NAME_POSTFIX);
	}

	public static <T> Proxy<T> of(Class<T> clazz) {
		return new Proxy<>(clazz);
	}

	public Proxy<T> with(Aspect... aspects) {
		for (Aspect aspect : aspects) {
			if (aspect.advice().getDeclaredFields().length > 0) {
				throw new IllegalArgumentException("Fields in advice are not supported.");
			}
			if (aspect.advice().getDeclaredMethods().length > 1) {
				throw new IllegalArgumentException("Exactly one method is required in advice.");
			}
		}
		this.aspects.addAll(Arrays.asList(aspects));
		return this;
	}

	public Proxy<T> debugPath(Path debugPath) {
		this.debugPath = debugPath;
		return this;
	}

	public byte[] create() {
		ClassModel cm = parseClass(targetClass);
		if (cm.flags().has(AccessFlag.FINAL)) {
			throw new IllegalArgumentException("Only non-final class can be proxied: " + targetClass);
		}
		byte[] bytes = ClassFile.of().build(proxyClassType, cb -> {
			cb.withFlags(cm.flags().flagsMask()).withSuperclass(cm.thisClass());
			cm.findAttributes(Attributes.SIGNATURE).forEach(cb::with);
			cm.findAttributes(Attributes.RUNTIME_VISIBLE_ANNOTATIONS).forEach(cb::with);
			for (MethodModel mm : cm.methods()) {
				String methodName = mm.methodName().stringValue();
				if (methodName.equals(ConstantDescs.CLASS_INIT_NAME)) {
					continue;
				}
				if (methodName.equals(ConstantDescs.INIT_NAME)) {
					proxyConstructor(cb, cm, mm);
				} else {
					proxyMethod(cb, cm, mm);
				}
			}
		});
		if (debugPath != null) {
			try {
				Files.write(debugPath.resolve(proxyClassType.displayName() + ".class"), bytes);
			} catch (IOException e) {
				throw new ProxyException(e);
			}
		}
		return bytes;
	}

	@SuppressWarnings("unchecked")
	public Class<T> load() {
		try {
			return (Class<T>) MethodHandles.privateLookupIn(targetClass, LOOKUP).defineClass(create());
		} catch (Exception e) {
			throw new ProxyException(e);
		}
	}

	public T instance() {
		try {
			var constructor = load().getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (NoSuchMethodException e) {
			throw new ProxyException("Default constructor is required for proxying " + targetClass);
		} catch (Exception e) {
			throw new ProxyException(e);
		}
	}

	private ClassModel parseClass(Class<?> clazz) {
		var in = ClassLoader.getSystemResourceAsStream(clazz.getName().replace('.', '/') + ".class");
		if (in == null) {
			throw new IllegalArgumentException(clazz + " not found.");
		}
		try (in) {
			return ClassFile.of().parse(in.readAllBytes());
		} catch (IOException e) {
			throw new ProxyException(e);
		}
	}

	private void copyAttributes(MethodBuilder builder, AttributedElement source) {
		source.findAttributes(Attributes.SIGNATURE).forEach(builder::with);
		source.findAttributes(Attributes.RUNTIME_VISIBLE_ANNOTATIONS).forEach(builder::with);
	}

	private void proxyConstructor(ClassBuilder cb, ClassModel cm, MethodModel mm) {
		if (!mm.methodName().equalsString(ConstantDescs.INIT_NAME) || mm.flags().has(AccessFlag.PRIVATE)) {
			return;
		}
		cb.withMethod(mm.methodName(), mm.methodType(), mm.flags().flagsMask(), mb -> {
			copyAttributes(mb, mm);
			mb.withCode(code -> {
				code.aload(0);
				loadArguments(code, mm.methodTypeSymbol());
				code.invokespecial(cm.thisClass().asSymbol(), ConstantDescs.INIT_NAME, mm.methodTypeSymbol());
				code.return_();
			});
		});
	}

	// methodName() -> methodName1() -> methodName2() -> ... -> super.methodName()
	private void proxyMethod(ClassBuilder cb, ClassModel cm, MethodModel mm) {
		List<Class<? extends Advice>> list = new ArrayList<>(aspects.size());
		for (Aspect aspect : aspects) {
			if (aspect.pointcut().apply(mm)) {
				AccessFlags flags = mm.flags();
				if (flags.has(AccessFlag.FINAL) || flags.has(AccessFlag.PRIVATE) || flags.has(AccessFlag.STATIC) || flags.has(AccessFlag.ABSTRACT)) {
					throw new IllegalArgumentException("Only non-final and non-private instance methods are allowed: " + mm);
				} else {
					list.add(aspect.advice());
				}
			}
		}
		String methodName = mm.methodName().stringValue();
		MethodTypeDesc methodType = mm.methodTypeSymbol();
		MethodTypeDesc nextMethodType = methodType.changeReturnType(ConstantDescs.CD_Object);
		String adviceMethodName = Advice.class.getDeclaredMethods()[0].getName();
		ClassDesc placeholderType = ClassDesc.of(ProxyTarget.class.getName());
		int slotOffset = methodType.parameterList().stream().map(type -> TypeKind.from(type).slotSize()).reduce(1, Integer::sum);
		for (int i = 0; i < list.size(); i++) {
			boolean isFirst = i == 0;
			boolean isLast = i == list.size() - 1;
			String currentMethodName = isFirst ? methodName : methodName + NAME_SEPARATOR + i;
			String nextMethodName = isLast ? methodName : methodName + NAME_SEPARATOR + (i + 1);
			ClassModel acm = parseClass(list.get(i));
			cb.withMethod(currentMethodName, isFirst ? methodType : nextMethodType, mm.flags().flagsMask(), mb -> {
				copyAttributes(mb, mm);
				mb.withCode(code -> {
					for (MethodModel amm : acm.methods()) {
						if (!amm.methodName().equalsString(adviceMethodName)) {
							continue;
						}
						CodeElement previous = NopInstruction.of();
						for (CodeElement ace : amm.code().orElseThrow()) {
							switch (ace) {
								// recalculate local variable slot for LOAD, STORE and INCREMENT instructions
								case LoadInstruction ins -> code.loadInstruction(ins.typeKind(), ins.slot() == 0 ? 0 : ins.slot() + slotOffset);
								case StoreInstruction ins -> code.storeInstruction(ins.typeKind(), ins.slot() == 0 ? 0 : ins.slot() + slotOffset);
								case IncrementInstruction ins -> code.incrementInstruction(ins.slot() == 0 ? 0 : ins.slot() + slotOffset, ins.constant());
								// replace ProxyTarget placeholder
								case InvokeInstruction ins when ins.opcode() == Opcode.INVOKESTATIC && ins.owner().asSymbol().equals(placeholderType) -> {
									switch (ins.method().name().stringValue()) {
										case "proceed" -> {
											code.aload(0);
											loadArguments(code, methodType);
											if (isLast) {
												code.invokespecial(cm.thisClass().asSymbol(), methodName, methodType);
												box(code, methodType.returnType());
											} else {
												code.invokevirtual(proxyClassType, nextMethodName, nextMethodType);
											}
										}
										case "name" -> code.ldc(cb.constantPool().stringEntry(methodName));
										case "argumentCount" -> pushInt(code, methodType.parameterCount());
										case "argumentTypes" -> loadArgumentClasses(code, methodType, cb);
										case "returnType" -> loadClass(code, methodType.returnType(), cb);
										case "argument" -> {
											code.pop();
											loadArgumentObject(code, methodType, getArgumentIndex(previous));
										}
										case "arguments" -> loadArgumentObjects(code, methodType);
										case "setArgument" -> {
											code.pop();
											storeArgumentObject(code, methodType, getArgumentIndex(previous));
										}
										default -> throw new ProxyException("Unsupported ProxyTarget method: " + ins.method().name());
									}
								}
								// unbox the result if necessary
								case ReturnInstruction ins when ins.opcode() == Opcode.ARETURN && isFirst -> {
									unbox(code, methodType.returnType());
									if (!methodType.returnType().isPrimitive()) {
										code.checkcast(methodType.returnType());
									}
									code.returnInstruction(TypeKind.from(methodType.returnType()));
								}
								default -> code.with(ace);
							}
							previous = ace;
						}
					}
				});
			});
		}
	}

	private void loadArguments(CodeBuilder code, MethodTypeDesc methodType) {
		var parameters = methodType.parameterArray();
		for (int i = 0; i < parameters.length; i++) {
			code.loadInstruction(TypeKind.from(parameters[i]), code.parameterSlot(i));
		}
	}

	private void loadClass(CodeBuilder code, ClassDesc type, ClassBuilder classBuilder) {
		if (type.isPrimitive()) {
			ClassDesc boxType = BOX_MAP.get(type);
			if (boxType != null) {
				code.getstatic(boxType, "TYPE", ConstantDescs.CD_Class);
			}
		} else {
			code.ldc(classBuilder.constantPool().classEntry(type));
		}
	}

	private void loadArgumentClasses(CodeBuilder code, MethodTypeDesc methodType, ClassBuilder classBuilder) {
		int argCount = methodType.parameterCount();
		pushInt(code, argCount);
		code.anewarray(ConstantDescs.CD_Class);
		for (int i = 0; i < argCount; i++) {
			code.dup();
			pushInt(code, i);
			loadClass(code, methodType.parameterType(i), classBuilder);
			code.aastore();
		}
	}

	private void loadArgumentObject(CodeBuilder code, MethodTypeDesc methodType, int index) {
		checkIndex(index, methodType.parameterCount());
		code.loadInstruction(TypeKind.from(methodType.parameterType(index)), code.parameterSlot(index));
		box(code, methodType.parameterType(index));
	}

	private void loadArgumentObjects(CodeBuilder code, MethodTypeDesc methodType) {
		int argCount = methodType.parameterCount();
		pushInt(code, argCount);
		code.anewarray(ConstantDescs.CD_Object);
		for (int i = 0; i < argCount; i++) {
			code.dup();
			pushInt(code, i);
			loadArgumentObject(code, methodType, i);
			code.aastore();
		}
	}

	private void storeArgumentObject(CodeBuilder code, MethodTypeDesc methodType, int index) {
		checkIndex(index, methodType.parameterCount());
		unbox(code, methodType.parameterType(index));
		code.storeInstruction(TypeKind.from(methodType.parameterType(index)), code.parameterSlot(index));
	}

	private int getArgumentIndex(CodeElement previous) {
		return switch (previous) {
			case ConstantInstruction.IntrinsicConstantInstruction ins -> ins.opcode().bytecode() - Opcode.ICONST_0.bytecode();
			case ConstantInstruction.ArgumentConstantInstruction ins -> ins.constantValue();
			default -> throw new ProxyException("Unexpected previous instruction used for setting argument index.");
		};
	}

	private void pushInt(CodeBuilder code, int value) {
		if (value <= 5) {
			code.with(ConstantInstruction.ofIntrinsic(Opcode.valueOf("ICONST_" + value)));
		} else if (value <= Byte.MAX_VALUE) {
			code.bipush(value);
		} else {
			code.sipush(value);
		}
	}

	private void checkIndex(int index, int length) {
		if (index < 0 || index >= length) {
			throw new IllegalArgumentException("index: %d (expected: 0 <= index < %d)".formatted(index, length));
		}
	}

	private void box(CodeBuilder code, ClassDesc type) {
		if (type.equals(ConstantDescs.CD_void)) {
			code.aconst_null();
		} else {
			ClassDesc boxType = BOX_MAP.get(type);
			if (boxType != null) {
				code.invokestatic(boxType, "valueOf", MethodTypeDesc.of(boxType, type));
			}
		}
	}

	private void unbox(CodeBuilder code, ClassDesc type) {
		if (type.equals(ConstantDescs.CD_Void)) {
			code.pop();
		} else {
			ClassDesc unboxType = UNBOX_MAP.get(type);
			if (unboxType != null) {
				code.checkcast(type);
				code.invokevirtual(type, unboxType.displayName() + "Value", MethodTypeDesc.of(unboxType));
			}
		}
	}
}
