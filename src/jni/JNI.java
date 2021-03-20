package jni;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

import static jdk.incubator.foreign.CLinker.*;
import static jni.JNIUtil.*;
import static jni.JNIType.*;

import java.lang.invoke.MethodHandle;

import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAccess;


public class JNI {
	
	private static final MethodHandle GET_CREATED_JAVA_VMS;
	static {
		GET_CREATED_JAVA_VMS =  lookup(
				LibraryLookup.ofDefault().lookup("JNI_GetCreatedJavaVMs").orElseThrow(),
				JNIFuncDesc.of(JINT, JPTR, JINT, JPTR));
	}
	
	public static JavaVM[] getCreatedJavaVMs() throws Throwable {
		try (var vmc_seg = MemorySegment.allocateNative(C_INT)) {
			checkOK((int) GET_CREATED_JAVA_VMS.invokeExact(MemoryAddress.NULL, 0, vmc_seg.address()));
			int oldvmc = MemoryAccess.getInt(vmc_seg);
			do {
				try (var vms = MemorySegment.allocateNative(MemoryLayout.ofSequence(oldvmc, C_POINTER))) {
					checkOK((int) GET_CREATED_JAVA_VMS.invokeExact(vms.address(), oldvmc, vmc_seg.address()));
					if (oldvmc != MemoryAccess.getInt(vmc_seg)) continue;
					JavaVM[] result = new JavaVM[oldvmc];
					for (int i = 0; i < oldvmc; i++) {
						result[i] = new JavaVM(MemoryAccess.getAddressAtIndex(vms, i));
					}
					return result;
				}
			} while (true);
		}
	}
}
