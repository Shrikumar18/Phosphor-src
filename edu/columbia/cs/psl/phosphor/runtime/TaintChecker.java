package edu.columbia.cs.psl.phosphor.runtime;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class TaintChecker {
	
	public static void checkTaint(int tag)
	{
		if(tag != 0)
			throw new IllegalAccessError("Argument carries taint " + tag);
	}
	
	
	//New check taint method will look at the level of the tag
	public static void checkTaint(Taint tag)
	{
		if (tag != null){
			if (tag.taintLevel == TaintLevel.TAINTED){
				throw new IllegalAccessError("Argument carries taint! " + tag);
			} else if (tag.taintLevel == TaintLevel.MAYBE_TAINTED){
				TaintLogger.logTaint(tag);
			}
		}
	}
	
	/*
	public static void checkTaint(Taint tag)
	{
		if(tag != null)
			throw new IllegalAccessError("Argument carries taint " + tag);
	}
	*/
	
	public static void checkTaint(Object obj) {
		if(obj == null)
			return;
		if (obj instanceof TaintedWithIntTag) {
			if (((TaintedWithIntTag) obj).getPHOSPHOR_TAG() != 0)
				throw new IllegalAccessError("Argument carries taint " + ((TaintedWithIntTag) obj).getPHOSPHOR_TAG());
		}
		else if (obj instanceof TaintedWithObjTag) {
			checkTaint(((TaintedWithObjTag) obj).getPHOSPHOR_TAG());
		}

		else if(obj instanceof int[])
		{
			for(int i : ((int[])obj))
			{
				if(i > 0)
					throw new IllegalAccessError("Argument carries taints - example: " +i);
			}
		}
		else if(obj instanceof LazyArrayIntTags)
		{
			LazyArrayIntTags tags = ((LazyArrayIntTags) obj);
			if (tags.taints != null)
				for (int i : tags.taints) {
					if (i > 0)
						throw new IllegalAccessError("Argument carries taints - example: " + i);
				}
		}
		else if(obj instanceof LazyArrayObjTags)
		{
			LazyArrayObjTags tags = ((LazyArrayObjTags) obj);
			if (tags.taints != null)
				for (Object i : tags.taints) {
					checkTaint(i);
				}
		}
		else if(obj instanceof Object[])
		{
			for(Object o : ((Object[]) obj))
				checkTaint(o);
		}
		else if(obj instanceof ControlTaintTagStack)
		{
			//TODO: Examine control taint tag stack
			ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
			if(ctrl.taint != null && !ctrl.isEmpty())
			{
				throw new IllegalAccessError("Current control flow carries taints:  " + ctrl.taint);
			}
		}
		else if(obj instanceof Taint)
		{
			checkTaint((Taint)obj);
		}
	}
	
	/*
	public static void checkTaint(Object obj) {
		if(obj == null)
			return;
		if (obj instanceof TaintedWithIntTag) {
			if (((TaintedWithIntTag) obj).getPHOSPHOR_TAG() != 0)
				throw new IllegalAccessError("Argument carries taint " + ((TaintedWithIntTag) obj).getPHOSPHOR_TAG());
		}
		else if (obj instanceof TaintedWithObjTag) {
			if (((TaintedWithObjTag) obj).getPHOSPHOR_TAG() != null)
				throw new IllegalAccessError("Argument carries taint " + ((TaintedWithObjTag) obj).getPHOSPHOR_TAG());
		}

		else if(obj instanceof int[])
		{
			for(int i : ((int[])obj))
			{
				if(i > 0)
					throw new IllegalAccessError("Argument carries taints - example: " +i);
			}
		}
		else if(obj instanceof LazyArrayIntTags)
		{
			LazyArrayIntTags tags = ((LazyArrayIntTags) obj);
			if (tags.taints != null)
				for (int i : tags.taints) {
					if (i > 0)
						throw new IllegalAccessError("Argument carries taints - example: " + i);
				}
		}
		else if(obj instanceof LazyArrayObjTags)
		{
			LazyArrayObjTags tags = ((LazyArrayObjTags) obj);
			if (tags.taints != null)
				for (Object i : tags.taints) {
					if (i != null)
						throw new IllegalAccessError("Argument carries taints - example: " + i);
				}
		}
		else if(obj instanceof Object[])
		{
			for(Object o : ((Object[]) obj))
				checkTaint(o);
		}
		else if(obj instanceof ControlTaintTagStack)
		{
			ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
			if(ctrl.taint != null && !ctrl.isEmpty())
			{
				throw new IllegalAccessError("Current control flow carries taints:  " + ctrl.taint);
			}
		}
		else if(obj instanceof Taint)
		{
			throw new IllegalAccessError("Argument carries taints:  " + obj);
		}
	}*/

	public static boolean hasTaints(int[] tags) {
		if(tags == null)
			return false;
		for (int i : tags) {
			if (i != 0)
				return true;
		}
		return false;
	}
	public static void setTaints(Object obj, int tag) {
		if(obj == null)
			return;
		if (obj instanceof TaintedWithIntTag) {
			((TaintedWithIntTag) obj).setPHOSPHOR_TAG(tag);
		} else if (obj instanceof LazyArrayIntTags){
			((LazyArrayIntTags)obj).setTaints(tag);
		} else if (obj.getClass().isArray()) {
			
				Object[] ar = (Object[]) obj;
				for (Object o : ar)
					setTaints(o, tag);
			
		}
		if(obj instanceof Iterable)
		{
			for(Object o : ((Iterable)obj))
				setTaints(o, tag);
		}
	}
	public static void setTaints(LazyCharArrayObjTags tags, Object tag) {
		if(tags.val.length == 0)
			return;
		tags.taints = new Taint[tags.val.length];
		for (int i = 0; i < tags.val.length; i++)
			tags.taints[i] = (Taint) tag;
	}
	public static void setTaints(Object obj, Taint tag) {
		
		
		if(obj == null){
			TaintLogger.logCustom("Null object with taint: " + tag);
		} else if (obj instanceof TaintedWithObjTag) {
			TaintLogger.logCustom("Tainted with obj tag: " + obj.toString() + " || with taint: " + tag);
			((TaintedWithObjTag) obj).setPHOSPHOR_TAG(tag);
		} else if (obj instanceof LazyArrayObjTags){
			TaintLogger.logCustom("Tainting lazy array obj tags: " + obj.toString() + " || with taint: " + tag);
			((LazyArrayObjTags)obj).setTaints(tag);
		} else if (obj.getClass().isArray()) {
				TaintLogger.logCustom("Tainting array: " + obj.toString() + " || with taint: " + tag);
				Object[] ar = (Object[]) obj;
				for (Object o : ar)
					setTaints(o, tag);
			
		}
		if(obj instanceof Iterable)
		{
			TaintLogger.logCustom("Tainting iterable: " + obj.toString() + " || with taint: " + tag);
			for(Object o : ((Iterable)obj))
				setTaints(o, tag);
		}
	}

	public static void sanitize(Object obj){
		
		Taint tag = null;
		if(obj == null){
			TaintLogger.logCustom("Sanitizing null object");
		} else if (obj instanceof TaintedWithObjTag) {
			tag = (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
			
			TaintLogger.logCustom("Sanitizing tainted with obj tag: " + obj + ", taint: " + tag);
			
			if (tag != null){
				tag.taintLevel = TaintLevel.lUB(tag.taintLevel);
				((TaintedWithObjTag) obj).setPHOSPHOR_TAG(tag);
			}
		} else if (obj instanceof TaintedPrimitiveWithObjTag){
			tag = (Taint) ((TaintedPrimitiveWithObjTag) obj).taint;
			
			TaintLogger.logCustom("Sanitizing tainted primitive with obj tag: " + obj + ", taint: " + tag);
			
			if (tag != null){
				tag.taintLevel = TaintLevel.lUB(tag.taintLevel);
				((TaintedPrimitiveWithObjTag) obj).taint = tag;
			}
		} else if (obj instanceof LazyArrayObjTags){
			TaintLogger.logCustom("Sanitizing lazy array obj tags: " + obj);
			((LazyArrayObjTags)obj).sanitizeTaints();
		} else if (obj.getClass().isArray()) {
				TaintLogger.logCustom("Sanitizing array with obj tag: " + obj);
				Object[] ar = (Object[]) obj;
				for (Object o : ar)
					sanitize(o);
			
		}
		if(obj instanceof Iterable)
		{
			TaintLogger.logCustom("Sanitizing iterable with obj tag: " + obj + ", taint: " + tag);
			for(Object o : ((Iterable)obj))
				sanitize(o);
		}
	}
	
	public static void setTaints(LazyCharArrayIntTags tags, int tag) {
		if(tags.val.length == 0)
			return;
		tags.taints = new int[tags.val.length];
		for (int i = 0; i < tags.val.length; i++)
			tags.taints[i] = tag;
	}
	public static void setTaints(Taint[] array, Taint tag) {
		if(array == null)
			return;
		for (int i = 0; i < array.length; i++)
			array[i] = tag;
	}
}
