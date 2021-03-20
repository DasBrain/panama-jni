package jni;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

import static jdk.incubator.foreign.CLinker.*;

import static jni.JNIUtil.*;
import static jni.JNIType.*;

public class JavaVM {
	
	private static class Functions {
		private final MemoryAddress base;
		
		private static final int METHOD_COUNT = 8;
		private static final MemoryLayout LAYOUT = MemoryLayout.ofSequence(METHOD_COUNT, C_POINTER);
		
		private static final int DestroyJavaVMIDX = 3;
		final MethodHandle DestroyJavaVM;
		private static final int DetachCurrentThreadIDX = 5;
		final MethodHandle DetachCurrentThread;
		private static final int GetEnvIDX = 6;
		final MethodHandle GetEnv;
		
		private Functions(MemoryAddress address) {
			base = address;
			try (var funcs = address.asSegmentRestricted(LAYOUT.byteSize())) {
				DetachCurrentThread = vtable(funcs, DetachCurrentThreadIDX, JINT, JPTR);
				GetEnv = vtable(funcs, GetEnvIDX, JINT, JPTR, JPTR, JINT);
				DestroyJavaVM = vtable(funcs, DestroyJavaVMIDX, JINT, JPTR);
			}
		}
		
		private static final ConcurrentHashMap<MemoryAddress, Functions> FUNCTIONS = new ConcurrentHashMap<>();
		
		static Functions of(MemoryAddress address) {
			return FUNCTIONS.computeIfAbsent(address, Functions::new);
		}
	}
	
	
	private final MemoryAddress base;
	private final Functions functions;
	
	private static final int JNI_VERSION_10 = 0x000a0000; // 10
	private static final int JVMTI_VERSION_11  = 0x300B0000; // 11
	private static final int JVMTI_VERSION = 0x30000000 + (17 * 0x10000) + ( 0 * 0x100) + 0;
	
	
	JavaVM(MemoryAddress address) throws Throwable {
		base = address;
		try (var funcs = address.asSegmentRestricted(C_POINTER.byteSize())) {
			functions = Functions.of(MemoryAccess.getAddress(funcs));
			
		}
	}
	
	public JNIEnv GetEnv() throws Throwable {
		try (var envPtr = MemorySegment.allocateNative(C_POINTER)) {
			checkOK((int) functions.GetEnv.invokeExact(base, envPtr.address(), JNI_VERSION_10));
			return new JNIEnv(MemoryAccess.getAddress(envPtr));
		}
	}
	
	public JVMTIEnv GetJVMTIEnv() throws Throwable {
		try (var envPtr = MemorySegment.allocateNative(C_POINTER)) {
			checkOK((int) functions.GetEnv.invokeExact(base, envPtr.address(), JVMTI_VERSION));
			return new JVMTIEnv(MemoryAccess.getAddress(envPtr));
		}
	}
	
	public void DetachCurrentThread() throws Throwable {
		checkOK((int) functions.DetachCurrentThread.invokeExact(base));
	}
	
	public void DestroyJavaVM() throws Throwable {
		checkOK((int) functions.DestroyJavaVM.invokeExact(base));
	}
	
	public static JavaVM current() {
		return JVMHolder.CURRENT;
	}
	
	static class JVMHolder {
		static final JavaVM CURRENT;
		static {
			try {
				CURRENT = JNI.getCreatedJavaVMs()[0];
			} catch (RuntimeException | Error e) {
				throw e;
			} catch (Throwable t) {
				throw new ExceptionInInitializerError(t);
			}
		}
	}
}
