package org.scalameter.javaApi;

public interface FlatmapFunction<T, S> {
	public JavaGenerator<S> flatmap(T t);
}
