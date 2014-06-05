package org.scalameter.javaApi;

import java.io.Serializable;

public interface Using<Input, Output> extends Serializable{
	public Output snippet(Input in);
	public JavaGenerator<Input> generator();
}
