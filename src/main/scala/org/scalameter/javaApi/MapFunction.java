package org.scalameter.javaApi;

import java.io.Serializable;

public interface MapFunction<T, S> extends Serializable{
	public S map(T t);
}
