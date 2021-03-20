package jni;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.MemoryAddress;

public class JObject implements Addressable, AutoCloseable {
	final MemoryAddress base;
	
	JObject(MemoryAddress address) {
		base = address;
	}
	
	public static JObject of(Object obj) {
		if (obj instanceof Class<?>) {
			return JClass.of((Class<?>) obj);
		}
		return new JObject(JNINatives.asGlobalRef(obj));
	}

	@Override
	public MemoryAddress address() {
		return base;
	}

	@Override
	public void close() throws Exception {
		try {
			JNIEnv.getEnv().DeleteGlobalRef(base);
		} catch (Exception | Error e) {
			throw e;
		} catch (Throwable t) {
			throw new InternalError(t);
		}
	}

	public Object deref() {
		return JNINatives.deref(base);
	}
}
