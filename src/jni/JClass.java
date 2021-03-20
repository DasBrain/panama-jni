package jni;

import jdk.incubator.foreign.MemoryAddress;

public class JClass extends JObject {

	JClass(MemoryAddress address) {
		super(address);
	}
	
	public static JClass of(Class<?> clazz) {
		return new JClass(JNINatives.asGlobalRef(clazz));
	}
	
	@Override
	public Class<?> deref() {
		return (Class<?>) super.deref();
	}
}
