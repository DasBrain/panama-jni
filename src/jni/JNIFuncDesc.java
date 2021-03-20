package jni;

import java.lang.invoke.MethodType;
import java.util.List;

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryLayout;

class JNIFuncDesc {
	private final JNIType returnType;
	private final List<JNIType> params;
	
	public static JNIFuncDesc of(JNIType returnType, JNIType... params) {
		return new JNIFuncDesc(returnType, List.of(params));
	}
	
	public static JNIFuncDesc of(JNIType returnValue, List<JNIType> params) {
		return new JNIFuncDesc(returnValue, List.copyOf(params));
	}
	
	private JNIFuncDesc(JNIType returnType, List<JNIType> params) {
		this.returnType = returnType;
		this.params = params;
	}
	
	public JNIType returnType() {
		return returnType;
	}
	
	public List<JNIType> parameters() {
		return params;
	}
	
	public MethodType toMethodType() {
		Class<?> retType = returnType.jtype();
		@SuppressWarnings({ "unchecked", "rawtypes" }) // This is safe.
		List<Class<?>> ps = (List) params.stream().map(JNIType::jtype).toList();
		return MethodType.methodType(retType, ps);
	}
	
	public FunctionDescriptor toFunctionDescriptor() {
		if (returnType.ntype() == null) {
			return FunctionDescriptor.ofVoid(params.stream().map(JNIType::ntype).toArray(MemoryLayout[]::new));
		}
		return FunctionDescriptor.of(returnType.ntype(), params.stream().map(JNIType::ntype).toArray(MemoryLayout[]::new));
	}
}
