package edu.columbia.cs.psl.phosphor.runtime;

public enum TaintLevel implements Comparable<TaintLevel> {
	UNTAINTED, 
	MAYBE_TAINTED, 
	TAINTED, 
	UNKNOWN;
	
}
