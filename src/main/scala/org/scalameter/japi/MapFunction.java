package org.scalameter.japi;



import java.io.Serializable;



public interface MapFunction<T, S> extends Serializable {
	public S map(T t);
}
