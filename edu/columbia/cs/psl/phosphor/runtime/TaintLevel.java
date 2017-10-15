package edu.columbia.cs.psl.phosphor.runtime;

public enum TaintLevel implements Comparable<TaintLevel> {
	UNTAINTED, 
	MAYBE_TAINTED, 
	TAINTED, 
	UNKNOWN;
	
	public String toString(){
		if (this == UNTAINTED){
			return "Untainted";
		} else if (this == MAYBE_TAINTED){
			return "Maybe Tainted";
		} else if (this == TAINTED){
			return "Tainted";
		} else {
			return "Unknown";
		}
	}
	
	//greatest lower bound of two taint labels
	public static final TaintLevel gLB(TaintLevel tl1, TaintLevel tl2){
		//these shouldn't really be null, but not sure how Phosphor initializes Taints yet.
		if (tl1 == null && tl2 != null){  
			return tl2;
		} else if (tl1 != null && tl2 == null){
			return tl1;
		} else {
			return tl1.compareTo(tl2) > 0 ? tl1 : tl2;
		}
	}
	
	public static final TaintLevel getLevelFromString(String levelIn){
		//TODO: add a null check
		
		if (levelIn.equalsIgnoreCase("untainted")){
			return UNTAINTED;
		} else if (levelIn.equalsIgnoreCase("maybe_tainted") || levelIn.equalsIgnoreCase("maybetainted")){
			return MAYBE_TAINTED;
		} else if (levelIn.equalsIgnoreCase("tainted")){
			return TAINTED;
		} else {
			return TAINTED; //this case should have a default instead of just assuming its tainted
		}
	}
	
		
	//for testing
	public static void main(String [] args){
		TaintLevel untainted = TaintLevel.UNTAINTED;
		TaintLevel maybe = TaintLevel.MAYBE_TAINTED;
		TaintLevel tainted = TaintLevel.TAINTED;
		
		System.out.println("GLB of " + untainted + " and " + maybe + ": " + TaintLevel.gLB(untainted, maybe));
		System.out.println("GLB of " + maybe + " and " + tainted + ": " + TaintLevel.gLB(tainted, maybe));
		System.out.println("GLB of " + tainted + " and " + untainted + ": " + TaintLevel.gLB(untainted, tainted));
	}
}
