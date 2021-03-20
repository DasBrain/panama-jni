package jni;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.loader.NativeLibraries;
import jdk.internal.loader.NativeLibrary;

class JNINativesBootstrap {
	
	private static final String LIB_NAME = "jni.natives.bootstrap";
	private static final MemorySegment bootstrapUpcall;
	static {
		try {
			bootstrapUpcall = CLinker.getInstance().upcallStub(JNIEnv.getEnv().functions.RegisterNatives, 
					FunctionDescriptor.of(CLinker.C_INT, CLinker.C_POINTER, CLinker.C_POINTER,
							CLinker.C_POINTER, CLinker.C_INT));
			
			injectLibrary();
		} catch (Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	private static void injectLibrary() throws Throwable {
		var nl = (NativeLibraries) MethodHandles
				.privateLookupIn(ClassLoader.class, MethodHandles.lookup())
				.findVarHandle(ClassLoader.class, "libraries", NativeLibraries.class)
				.get(JNINativesBootstrap.class.getClassLoader());
			var fnllibs = NativeLibraries.class.getDeclaredField("libraries");
			fnllibs.setAccessible(true);
			@SuppressWarnings("unchecked")
			var map = (Map<String, NativeLibrary>) fnllibs.get(nl);
			map.put(LIB_NAME, new LibraryImpl());
	}
	
	public static void bootstrap(Lookup target) throws Throwable {
	}
	
	private static native void bootstrap0(Class<?> target, String name, long upcall);
	
	private static class LibraryImpl implements NativeLibrary {

		@Override
		public String name() {
			return LIB_NAME;
		}

		@Override
		public long find(String name) {
			if (name.endsWith("_registerNatives__JI")) {
				return bootstrapUpcall.address().toRawLongValue();
			}
			return 0;
		}
		
	}
}
