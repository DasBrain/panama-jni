package jni;

import static jdk.incubator.foreign.CLinker.C_POINTER;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jni.JNIType.JINT;
import static jni.JNIType.JPTR;
import static jni.JNIType.JVOID;
import static jni.JNIUtil.vtable;

import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.NativeScope;

public class JVMTIEnv {
	static class Functions {
		
		private static final int METHOD_COUNT = 234;
		private static final MemoryLayout LAYOUT = MemoryLayout.ofSequence(METHOD_COUNT, C_POINTER);
		
		private static final int DeallocateIDX = 47;
		final MethodHandle Deallocate;
		private static final int GetSystemPropertiesIDX = 130;
		final MethodHandle GetSystemProperties;
		private static final int GetSystemPropertyIDX = 131;
		final MethodHandle GetSystemProperty;
			
		
		private Functions(MemoryAddress address) {
			try (var funcs = address.asSegmentRestricted(LAYOUT.byteSize())) {
				Deallocate = vtable(funcs, DeallocateIDX, JINT, JPTR, JPTR);
				GetSystemProperties = vtable(funcs, GetSystemPropertiesIDX, JINT, JPTR, JPTR, JPTR);
				GetSystemProperty = vtable(funcs, GetSystemPropertyIDX, JINT, JPTR, JPTR, JPTR);
			}
		}
		
		private static final ConcurrentHashMap<MemoryAddress, Functions> FUNCTIONS = new ConcurrentHashMap<>();
		
		static Functions of(MemoryAddress address) {
			return FUNCTIONS.computeIfAbsent(address, Functions::new);
		}
	}
	
	final MemoryAddress base;
	final Functions functions;

	public JVMTIEnv(MemoryAddress address) {
		base = address;
		try (var env = address.asSegmentRestricted(C_POINTER.byteSize())) {
			 functions = Functions.of(MemoryAccess.getAddress(env));
		}
	}
	
	public static JVMTIEnv getEnv() throws Throwable {
		return JavaVM.current().GetJVMTIEnv();
	}
	
	public String[] GetSystemProperties() throws Throwable {
		int count;
		MemoryAddress charPtr; 
		try (var ns = NativeScope.unboundedScope()) {
			var countPtr = ns.allocate(C_INT);
			var charPtrPtr = ns.allocate(C_POINTER);
			checkOK((int) functions.GetSystemProperties.invokeExact(base, countPtr.address(), charPtrPtr.address()));
			count = MemoryAccess.getInt(countPtr);
			charPtr = MemoryAccess.getAddress(charPtrPtr);
		}
		String[] result = new String[count];
		var strings = charPtr.asSegmentRestricted(MemoryLayout.ofSequence(count, C_POINTER).byteSize());
		for (int i = 0; i < count; i++) {
			var addr = MemoryAccess.getAddressAtIndex(strings, i);
			result[i] = CLinker.toJavaStringRestricted(addr, StandardCharsets.UTF_8);
			checkOK((int) functions.Deallocate.invokeExact(base, addr));
		}
		checkOK((int) functions.Deallocate.invokeExact(base, charPtr));
		return result;
	}
	
	public String GetSystemProperty(String name) throws Throwable {
		MemoryAddress charPtr;
		try (var ns = NativeScope.unboundedScope()) {
			var charPtrPtr = ns.allocate(C_POINTER);
			var namePtr = CLinker.toCString(name, StandardCharsets.UTF_8, ns);
			checkOK((int) functions.GetSystemProperty.invokeExact(base, namePtr.address(), charPtrPtr.address()));
			charPtr = MemoryAccess.getAddress(charPtrPtr);
		}
		String result = CLinker.toJavaStringRestricted(charPtr, StandardCharsets.UTF_8);
		checkOK((int) functions.Deallocate.invokeExact(base, charPtr));
		return result;
	}
	
	private static final void checkOK(int result) throws Exception {
		if (result != 0) throw new Exception("JVMTI Error: " + result);
	}
}
