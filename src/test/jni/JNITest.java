package test.jni;

import java.lang.invoke.MethodHandles;

import jni.JClass;
import jni.JNI;
import jni.JNIEnv;
import jni.JavaVM;

public class JNITest {
	public static void main(String[] args) throws Throwable {
		var env = JNIEnv.getEnv();
		var cl = JClass.of(MethodHandles.class);
		System.out.println(env.GetObjectRefType(cl.address()));
		System.out.println(cl.address());
		System.out.println(cl.deref());
		var mid = env.GetStaticMethodID(cl.address(), "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;");
		
		
	}
}
