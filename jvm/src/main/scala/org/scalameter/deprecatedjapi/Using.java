package org.scalameter.deprecatedjapi;



import java.io.Serializable;



/** Used to declare the benchmarked snippet in a Java performance test.
 */
public interface Using<Input, Output> extends Serializable {
	public Output snippet(Input in);
	public JavaGenerator<Input> generator();
}
