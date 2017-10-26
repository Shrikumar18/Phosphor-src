package edu.columbia.cs.psl.phosphor;

public class NullSourceSinkManager extends SourceSinkManager{

	@Override
	public boolean isSource(String str) {
		return false;
	}

	@Override
	public boolean isSink(String str) {
		return false;
	}

	@Override
	public Object getLabel(String str) {
		return null;
	}

	@Override
	public Object getLevel(String source) {
		return null;
	}

	@Override
	public boolean isSanitizer(String san) {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}
