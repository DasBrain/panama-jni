package jni;

import jdk.incubator.foreign.MemoryAddress;

public class JMethodID {
	final MemoryAddress base;
	JMethodID(MemoryAddress address) {
		this.base = address;
	}
}
