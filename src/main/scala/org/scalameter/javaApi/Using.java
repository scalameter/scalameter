package org.scalameter.javaApi;

public interface Using<Input, Output> {
	public Output snippet(Input in);
	public JavaGenerator<Input> generator();
}
