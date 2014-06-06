package org.scalameter.examples;

import java.util.HashMap;

import org.scalameter.OnlineRegressionReport;
import org.scalameter.javaApi.Collections;
import org.scalameter.javaApi.JavaGenerator;
import org.scalameter.javaApi.MapFunction;
import org.scalameter.javaApi.Persistor;
import org.scalameter.javaApi.RangeGen;
import org.scalameter.javaApi.SerializationPersistor;
import org.scalameter.javaApi.exec;
import org.scalameter.javaApi.Group;

public class JavaRegressionTest extends OnlineRegressionReport{
	public Persistor javaPersistor(){
		return new SerializationPersistor();
	}
	public class Array implements Group{
		public class foreach implements org.scalameter.javaApi.Using<int[], Integer>{
			public HashMap<exec, Object> config(){
				HashMap<exec, Object> config = new HashMap<exec, Object>();
				config.put(exec.independentSamples, 6);
				return config;
			}
			public JavaGenerator<int[]> generator() {
				JavaGenerator<Integer> sizes = new RangeGen("size", 1000000, 5000000, 2000000);
				JavaGenerator<String> s = sizes.map(new MapFunction<Integer, String>(){
					public String map(Integer t) {
						return t.toString();
					}
				});
				return new Collections(sizes).arrays();
			}
			public Integer snippet(int[] in){
				int sum = 0;
				for(int i = 0; i < in.length; i++){
					sum += in[i];
				}
				return sum;
			}
		}
	}
}
