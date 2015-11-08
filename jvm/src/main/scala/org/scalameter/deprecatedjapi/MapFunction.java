package org.scalameter.deprecatedjapi;



import java.io.Serializable;



public interface MapFunction<T, S> extends Serializable {
	public S map(T t);
}
