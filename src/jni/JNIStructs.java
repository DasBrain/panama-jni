package jni;

import jdk.incubator.foreign.MemoryLayout;

import static jdk.incubator.foreign.CLinker.*;

public interface JNIStructs {
	MemoryLayout JNINativeMethod = MemoryLayout.ofStruct(
		C_POINTER.withName("name"),
		C_POINTER.withName("signature"),
		C_POINTER.withName("fnPtr")
	).withName("JNINativeMethod");
}
