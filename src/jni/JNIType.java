package jni;

import static jdk.incubator.foreign.CLinker.*;

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryAddress;

enum JNIType {
	JVOID(void.class, null),
	JINT(int.class, C_INT),
	JPTR(MemoryAddress.class, C_POINTER),
	JBOOLEAN(boolean.class, C_CHAR),
	JCHAR(char.class, C_SHORT);
	 
	
	private final Class<?> jtype;
	private final MemoryLayout ntype;
	
	JNIType(Class<?> jtype, MemoryLayout ntype) {
		this.jtype = jtype;
		this.ntype = ntype;
	}
	
	public Class<?> jtype() {
		return jtype;
	}
	
	public MemoryLayout ntype() {
		return ntype;
	}
}
