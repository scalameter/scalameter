package org.scalameter.examples;



import java.io.Serializable;
import java.util.HashMap;
import org.scalameter.japi.*;



public class JMemoryTest extends OnlineRegressionReport {
	public Persistor javaPersistor() {
		return new SerializationPersistor();
	}
	public Measurer javaMeasurer(){
		return new org.scalameter.japi.MemoryFootprintMeasurer();
	}
	public class MemoryFootprint implements Group {
		public final JContext config = JContext.create()
			.put("exec.benchRuns", 10)
			.put("exec.independentSamples", 2);
		public class Array implements org.scalameter.japi.Using<Integer, int[]> {
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
