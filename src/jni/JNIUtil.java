package jni;

import java.lang.invoke.MethodHandle;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;

class JNIUtil {
	static void checkOK(int result) {
		if (result != 0) {
			throw new RuntimeException("JNI call went wrong. errno: " + result);
		}
	}
	
	static MethodHandle vtable(MemorySegment seg, long idx, JNIType retType, JNIType... argTypes) {
		return lookup(MemoryAccess.getAddressAtIndex(seg, idx),
				JNIFuncDesc.of(retType, argTypes));
	}
	
	static MethodHandle lookup(Addressable addr, JNIFuncDesc desc) {
		return CLinker.getInstance().downcallHandle(addr, desc.toMethodType(), desc.toFunctionDescriptor());
	}
}
