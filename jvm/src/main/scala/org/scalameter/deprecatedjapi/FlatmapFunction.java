package org.scalameter.deprecatedjapi;



import java.io.Serializable;



public interface FlatmapFunction<T, S> extends Serializable {
	public JavaGenerator<S> flatmap(T t);
}
