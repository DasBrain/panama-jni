package test.jni;

import jni.JVMTIEnv;

public class JVMTITest {
	public static void main(String[] args) throws Throwable {
		JVMTIEnv env = JVMTIEnv.getEnv();
		for (String prop : env.GetSystemProperties()) {
			System.out.println(prop + ": " + env.GetSystemProperty(prop));
		}
	}
}
