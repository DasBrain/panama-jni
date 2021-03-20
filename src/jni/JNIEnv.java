package jni;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import static jdk.incubator.foreign.CLinker.*;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAccess;

import static jni.JNIType.*;
import static jni.JNIUtil.*;

public class JNIEnv {
	
	static class Functions {
		
		private static final int METHOD_COUNT = 234;
		private static final MemoryLayout LAYOUT = MemoryLayout.ofSequence(METHOD_COUNT, C_POINTER);
		
		private static final int FindClassIDX = 6;
		final MethodHandle FindClass;
		private static final int NewGlobalRefIDX = 21;
		final MethodHandle NewGlobalRef;
		private static final int DeleteGlobalRefIDX = 22;
		final MethodHandle DeleteGlobalRef;
		private static final int GetStaticMethodIDIDX = 113;
		final MethodHandle GetStaticMethodID;
		private static final int GetStringUTFCharsIDX = 169;
		final MethodHandle GetStringUTFChars;
		private static final int ReleaseStringUTFCharsIDX = 170;
		final MethodHandle ReleaseStringUTFChars;
		private static final int RegisterNativesIDX = 215;
		final MethodHandle RegisterNatives;
		private static final int GetObjectRefTypeIDX = 232;
		final MethodHandle GetObjectRefType;
		
		private Functions(MemoryAddress address) {
			try (var funcs = address.asSegmentRestricted(LAYOUT.byteSize())) {
				FindClass = vtable(funcs, FindClassIDX, JPTR, JPTR, JPTR);
				NewGlobalRef = vtable(funcs, NewGlobalRefIDX, JPTR, JPTR, JPTR);
				DeleteGlobalRef = vtable(funcs, DeleteGlobalRefIDX, JVOID, JPTR, JPTR);
				GetStaticMethodID = vtable(funcs, GetStaticMethodIDIDX, JPTR, JPTR, JPTR, JPTR, JPTR);
				GetStringUTFChars = vtable(funcs, GetStringUTFCharsIDX, JPTR, JPTR, JPTR, JPTR);
				ReleaseStringUTFChars = vtable(funcs, ReleaseStringUTFCharsIDX, JVOID, JPTR, JPTR, JPTR);
				RegisterNatives = vtable(funcs, RegisterNativesIDX, JINT, JPTR, JPTR, JPTR, JINT);
				GetObjectRefType = vtable(funcs, GetObjectRefTypeIDX, JINT, JPTR, JPTR);
			}
		}
		
		private static final ConcurrentHashMap<MemoryAddress, Functions> FUNCTIONS = new ConcurrentHashMap<>();
		
		static Functions of(MemoryAddress address) {
			return FUNCTIONS.computeIfAbsent(address, Functions::new);
		}
	}
	
	final MemoryAddress base;
	final Functions functions;

	public JNIEnv(MemoryAddress address) {
		base = address;
		try (var env = address.asSegmentRestricted(C_POINTER.byteSize())) {
			 functions = Functions.of(MemoryAccess.getAddress(env));
		}
	}
	
	public static JNIEnv getEnv() throws Throwable {
		return JavaVM.current().GetEnv();
	}
	
	
	public MemoryAddress FindClass(String name) throws Throwable {
		try (var cname = CLinker.toCString(name)) {
			return (MemoryAddress) functions.FindClass.invokeExact(base, cname.address());
		}
	}
	
	public MemoryAddress NewGlobalRef(MemoryAddress obj) throws Throwable {
		return (MemoryAddress) functions.NewGlobalRef.invokeExact(base, obj);
	}
	
	public void DeleteGlobalRef(MemoryAddress obj) throws Throwable {
		functions.DeleteGlobalRef.invokeExact(base, obj);
	}
	public MemoryAddress GetStaticMethodID(MemoryAddress clazz, String name, String signature) throws Throwable {
		try (var cname = CLinker.toCString(name);
			 var csig = CLinker.toCString(signature)) {
			return (MemoryAddress) functions.GetStaticMethodID.invokeExact(base, clazz, cname.address(), csig.address());
		}
	}
	
	public MemoryAddress GetStringUTFChars(MemoryAddress string) throws Throwable {
		return (MemoryAddress) functions.GetStringUTFChars.invokeExact(base, string, MemoryAddress.NULL);
	}
	
	public void ReleaseStringUTFChars(MemoryAddress string, MemoryAddress charPtr) throws Throwable {
		functions.ReleaseStringUTFChars.invokeExact(base, string, charPtr);
	}
	
	public void RegisterNatives(MemoryAddress clazz, MemoryAddress methods, int nMethods) throws Throwable {
		
	}
	
	public int GetObjectRefType(MemoryAddress ref) throws Throwable {
		return (int) functions.GetObjectRefType.invokeExact(base, ref);
	}
}
