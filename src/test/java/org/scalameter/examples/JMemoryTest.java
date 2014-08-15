package org.scalameter.examples;

import java.io.Serializable;
import java.util.HashMap;

import org.scalameter.OnlineRegressionReport;
import org.scalameter.javaApi.Group;
import org.scalameter.javaApi.JavaGenerator;
import org.scalameter.javaApi.Measurer;
import org.scalameter.javaApi.Persistor;
import org.scalameter.javaApi.RangeGen;
import org.scalameter.javaApi.SerializationPersistor;
import org.scalameter.javaApi.exec;

public class JMemoryTest extends OnlineRegressionReport{
	public Persistor javaPersistor(){
		return new SerializationPersistor();
	}
	public Measurer javaMeasurer(){
		return new org.scalameter.javaApi.MemoryFootprint();
  }
	public class MemoryFootprint implements Group{
		public HashMap<exec, Object> config(){
			HashMap<exec, Object> config = new HashMap<exec, Object>();
			config.put(exec.benchRuns, 10);
			config.put(exec.independentSamples, 2);
			return config;
		}
		public class Array implements org.scalameter.javaApi.Using<Integer, int[]>{

			public int[] snippet(Integer in) {
				int[] array = new int[in];
				for(int i = 0; i < in; i++){
					array[i] = i;
				}
				return array;
			}

			public JavaGenerator<Integer> generator() {
				return new RangeGen("size", 1000000, 5000000, 2000000);
			}
			
		}
		
	}
}
