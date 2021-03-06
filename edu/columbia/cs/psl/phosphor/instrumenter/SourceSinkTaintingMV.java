package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.BasicSourceSinkManager;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.SourceSinkManager;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;

public class SourceSinkTaintingMV extends MethodVisitor implements Opcodes {
	static SourceSinkManager sourceSinkManager = BasicSourceSinkManager.getInstance();

	String owner;
	String name;
	String desc;
	boolean thisIsASource;
	boolean thisIsASink;
	
	boolean thisIsASanitizer;
	
	String origDesc;
	int access;
	boolean isStatic;
	Object lbl;
	Object level; //introduction of taint level declaration

	public SourceSinkTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc, String origDesc) {
		super(ASM5, mv);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.access = access;
		this.origDesc = origDesc;
		this.thisIsASource = sourceSinkManager.isSource(owner, name, desc);
		this.thisIsASink = sourceSinkManager.isSink(owner, name, desc);
		this.thisIsASanitizer = sourceSinkManager.isSanitizer(owner, name, desc);
		this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
		if (this.thisIsASource) {
			lbl = sourceSinkManager.getLabel(owner, name, desc);
			level = sourceSinkManager.getLevel(lbl.toString());
			String levelString = level != null ? " Level: " + level : "";
			System.out.println("Source: " + owner + "." + name + desc + " Label: " + lbl + levelString);
		}
		if (this.thisIsASink)
			System.out.println("Sink: " + owner + "." + name + desc);
		
		if (this.thisIsASanitizer)
			System.out.println("Sanitizer: " + owner + "." + name + desc);
	}

	/*
	 * As of 10/15/17: TaintLevels can be explicitly declared in taint-sources.txt
	 * If no level is declared, TAINTED is the default level
	 */
	private void loadSourceLblAndMakeTaint() {
		if (Configuration.MULTI_TAINTING) {
			super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "taintTagFactory", Type.getDescriptor(TaintTagFactory.class));
			super.visitLdcInsn(lbl);
			if (level != null){ //if there is a level explicitly declared, we will initialize the TaintLevel of Taint with it
				super.visitLdcInsn(level);
				super.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(TaintTagFactory.class), "getTaintWithLevel", "(Ljava/lang/String;Ljava/lang/String;)"+Configuration.TAINT_TAG_DESC, true);
			} else {
				super.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(TaintTagFactory.class), "getAutoTaint", "(Ljava/lang/String;)"+Configuration.TAINT_TAG_DESC, true);
			}
		} else {
			super.visitLdcInsn(lbl);
		}
	}

	@Override
	public void visitCode() {
		super.visitCode();
		if (this.thisIsASource) {
			Type[] args = Type.getArgumentTypes(desc);
			int idx = 0;
			if (!isStatic)
				idx++;
			boolean skipNextArray = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].getSort() == Type.OBJECT) {
					super.visitVarInsn(ALOAD, idx);
					loadSourceLblAndMakeTaint();
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				} else if (!skipNextArray && args[i].getSort() == Type.ARRAY
						&& (args[i].getElementType().getSort() != Type.OBJECT || args[i].getDescriptor().equals(Configuration.TAINT_TAG_ARRAYDESC)) && args[i].getDimensions() == 1) {
					skipNextArray = true;
					super.visitVarInsn(ALOAD, idx);
					loadSourceLblAndMakeTaint();
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(" + Configuration.TAINT_TAG_ARRAYDESC + Configuration.TAINT_TAG_DESC + ")V", false);
					//super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Sanitizer.class), "sanitizeTaint", "(" + Configuration.TAINT_TAG_DESC + ")V", false);
				} else if (skipNextArray)
					skipNextArray = false;
				idx += args[i].getSize();
			}
		}
		
		if(thisIsASanitizer){
			Type[] args = Type.getArgumentTypes(desc);
			int idx = 0;
			if (!isStatic)
				idx++;
			boolean skipNextArray = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].getSort() == Type.OBJECT && !args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC) || args[i].getSort() == Type.ARRAY) {
					super.visitVarInsn(ALOAD, idx);
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "sanitize", "(Ljava/lang/Object;)V", false);
				} else if (!skipNextArray && args[i].getSort() == Type.ARRAY
						&& (args[i].getElementType().getSort() != Type.OBJECT || args[i].getDescriptor().equals(Configuration.TAINT_TAG_ARRAYDESC)) && args[i].getDimensions() == 1) {
					skipNextArray = true;
					super.visitVarInsn(ALOAD, idx);
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "sanitize", "(Ljava/lang/Object;)V", false);
				} else if (skipNextArray)
					skipNextArray = false;
				idx += args[i].getSize();
				
			}
		}
		
		if (sourceSinkManager.isSink(owner, name, desc)) {
			//TODO - check every arg to see if is taint tag
			Type[] args = Type.getArgumentTypes(desc);
			int idx = 0;
			if (!isStatic)
				idx++;
			boolean skipNextPrimitive = false;
			for (int i = 0; i < args.length; i++) {
				if ((args[i].getSort() == Type.OBJECT && !args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC)) || args[i].getSort() == Type.ARRAY) {
					if (args[i].getSort() == Type.ARRAY && (args[i].getElementType().getSort() != Type.OBJECT || args[i].getDescriptor().equals(Configuration.TAINT_TAG_ARRAYDESC))
							&& args[i].getDimensions() == 1) {
						if (!skipNextPrimitive) {
							super.visitVarInsn(ALOAD, idx);
							super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "checkTaint", "(Ljava/lang/Object;)V", false);
						}
						skipNextPrimitive = !skipNextPrimitive;
					} else {
						super.visitVarInsn(ALOAD, idx);
						super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "checkTaint", "(Ljava/lang/Object;)V", false);
					}
				} else if (!skipNextPrimitive) {
					super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "checkTaint", "(" + Configuration.TAINT_TAG_DESC + ")V", false);
					skipNextPrimitive = true;
				} else if (skipNextPrimitive)
					skipNextPrimitive = false;
				idx += args[i].getSize();
			}
		}
		
		//if (thisIsASanitizer){
		
		//}
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode == ARETURN && this.thisIsASource) {
			Type returnType = Type.getReturnType(this.origDesc);
			if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
				super.visitInsn(DUP);
				loadSourceLblAndMakeTaint();
				super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
			} else if (returnType.getSort() == Type.VOID) {

			} else {
				//primitive
				super.visitInsn(DUP);
				loadSourceLblAndMakeTaint();
				if (Configuration.MULTI_TAINTING)
					super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitiveWithObjTag.class), "taint", Configuration.TAINT_TAG_DESC);
				else
					super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitiveWithIntTag.class), "taint", "I");
			}
		} else if (opcode == ARETURN && this.thisIsASanitizer){
			Type returnType = Type.getReturnType(this.origDesc);
			if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
				super.visitInsn(DUP);
				super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "sanitize", "(Ljava/lang/Object;)V", false);
			} else if (returnType.getSort() == Type.VOID) {

			} else {
				//TODO: primitives are getting sanitized twice, is this okay?
				super.visitInsn(DUP); //?
				if (Configuration.MULTI_TAINTING)
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "sanitize", "(Ljava/lang/Object;)V", false);
			}
		}
		super.visitInsn(opcode);
	}
}
