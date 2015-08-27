package org.scalameter.examples;



import org.scalameter.deprecatedjapi.*;



public class JMemoryTest extends OfflineReport {
	public Persistor javaPersistor() {
		return new SerializationPersistor();
	}
	public Measurer javaMeasurer(){
		return new org.scalameter.deprecatedjapi.MemoryFootprintMeasurer();
	}
	public class MemoryFootprint implements Group {
		public final JContext config = JContext.create()
			.put("exec.benchRuns", 10)
			.put("exec.independentSamples", 1);
		public class Array implements org.scalameter.deprecatedjapi.Using<Integer, int[]> {
			public int[] snippet(Integer in) {
				int[] array = new int[in];
				for(int i = 0; i < in; i++){
					array[i] = i;
				}
				return array;
			}

			public JavaGenerator<Integer> generator() {
				return new RangeGen("size", 100000, 500000, 200000);
			}
			
		}
		
	}
}
