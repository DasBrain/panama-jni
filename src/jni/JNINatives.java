package jni;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.NativeScope;

import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jni.JNIType.*;
import static java.lang.invoke.MethodType.methodType;

public class JNINatives {
	
	private static class NativeFunction {
		private static final VarHandle
			NAME = JNIStructs.JNINativeMethod.varHandle(long.class, groupElement("name")),
			SIGNATURE = JNIStructs.JNINativeMethod.varHandle(long.class, groupElement("signature")),
			FN = JNIStructs.JNINativeMethod.varHandle(long.class, groupElement("fnPtr"));
		
		private final String name;
		private final String signature;
		private final MemoryAddress upcall; 
		
		NativeFunction(String name, String signature, MemoryAddress upcall) {
			this.name = name;
			this.signature = signature;
			this.upcall = upcall;
		}

		public void copyTo(MemorySegment seg, NativeScope scope) {
			NAME.set(seg, CLinker.toCString(name, StandardCharsets.UTF_8, scope).address().toRawLongValue());
			SIGNATURE.set(seg, CLinker.toCString(signature, StandardCharsets.UTF_8, scope).address().toRawLongValue());
			FN.set(seg, upcall.toRawLongValue());
		}
	}
	
	private static final Lookup LOOKUP = MethodHandles.lookup();
	private static final CLinker LINKER = CLinker.getInstance();
	private static NativeFunction nf(String name, MethodType mt, JNIType returnType, JNIType... params) {
		try {
			JNIType[] mparams = new JNIType[params.length + 2];
			System.arraycopy(params, 0, mparams, 2, params.length);
			mparams[0] = JPTR;
			mparams[1] = JPTR;
			JNIFuncDesc funcDesc = JNIFuncDesc.of(returnType, mparams);
			var upMH = LOOKUP.findStatic(JNINatives.class, name + "Upcall", funcDesc.toMethodType());
			var up = LINKER.upcallStub(upMH, funcDesc.toFunctionDescriptor()).address();
			return new NativeFunction(name, mt.descriptorString(), up);
		} catch (Throwable t) {
			throw new ExceptionInInitializerError(t);
		}
	}
	
	static {
		try {
			JNINativesBootstrap.bootstrap(MethodHandles.lookup());
			NativeFunction[] funcs = {
				nf("asGlobalRef0", methodType(long.class, Object.class), JPTR, JPTR),
				nf("deref0", methodType(Object.class, long.class), JPTR, JPTR)
			};
			MemoryLayout methods = MemoryLayout.ofSequence(funcs.length, JNIStructs.JNINativeMethod);
			var boffh = methods.byteOffsetHandle(PathElement.sequenceElement());
			try (var scope = NativeScope.unboundedScope()) {
				var methmem = scope.allocate(methods);
				for (int i = 0; i < funcs.length; i++) {
					var slice = methmem.asSlice((long) boffh.invokeExact((long) i));
					funcs[i].copyTo(slice, scope);
				}
				registerNatives(methmem.address().toRawLongValue(), funcs.length);
			}
		} catch (Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	private static native int registerNatives(long methods, int nMethods);
	
	public static MemoryAddress asGlobalRef(Object o) {
		Objects.requireNonNull(o);
		return MemoryAddress.ofLong(asGlobalRef0(o));
	}
	
	private static native long asGlobalRef0(Object o);
	private static MemoryAddress asGlobalRef0Upcall(MemoryAddress env, MemoryAddress clazz, MemoryAddress obj) throws Throwable {
		return new JNIEnv(env).NewGlobalRef(obj);
	}
	
	public static Object deref(MemoryAddress ref) {
		long addr = ref.toRawLongValue();
		if (addr == 0L) {
			return null;
		}
		return deref0(addr);
	}
	
	private static native Object deref0(long ref);
	private static MemoryAddress deref0Upcall(MemoryAddress env, MemoryAddress clazz, MemoryAddress ref) {
		return ref;
	}
	
}
