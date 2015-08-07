package org.scalameter.examples;



import org.scalameter.deprecatedjapi.*;



public class JavaRegressionTest extends OnlineRegressionReport {
	public Persistor javaPersistor() {
		return new SerializationPersistor();
	}
	public class Array implements Group {
		public class foreach implements org.scalameter.deprecatedjapi.Using<int[], Integer> {
			public final JContext config = JContext.create().put("exec.independentSamples", 6);
			public JavaGenerator<int[]> generator() {
				JavaGenerator<Integer> sizes = new RangeGen("size", 1000000, 5000000, 2000000);
				JavaGenerator<String> s = sizes.map(new MapFunction<Integer, String>() {
					public String map(Integer t) {
						return t.toString();
					}
				});
				return new CollectionGenerators(sizes).arrays();
			}
			public Integer snippet(int[] in) {
				int sum = 0;
				for(int i = 0; i < in.length; i++) {
					sum += in[i];
				}
				return sum;
			}
		}
	}
}
