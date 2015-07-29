package org.scalameter.examples;



import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.scalameter.deprecatedjapi.*;



public class JavaRegressionTest3 extends OfflineReport {
	public Persistor javaPersistor() {
		return new SerializationPersistor();
	}
	public class List implements Group {
		public class groupBy implements org.scalameter.deprecatedjapi.Using<LinkedList<Integer>, HashMap<Integer, LinkedList<Integer>>> {
			public final JContext config = JContext.create()
			  .put("exec.benchRuns", 20)
			  .put("exec.independentSamples", 1)
			  .put("exec.outliers.covMultiplier", 1.5)
			  .put("exec.outliers.suspectPercent", 40);
			public JavaGenerator<LinkedList<Integer>> generator() {
				JavaGenerator<Integer> sizes = new IntSingleGen("size", 5000000);
				return new CollectionGenerators(sizes).lists();
			}
			public HashMap<Integer, LinkedList<Integer>> snippet(LinkedList<Integer> in) {
				HashMap<Integer, LinkedList<Integer>> hm = new HashMap<Integer, LinkedList<Integer>>();
				for(int i = 0; i < 10; i++) {
					hm.put(i, new LinkedList<Integer>());
				}
				Iterator<Integer> it = in.iterator();
				while (it.hasNext()) {
					Integer element = it.next();
					LinkedList<Integer> tmp = hm.get(element % 10);
					tmp.add(element);
					hm.put(element % 10, tmp);
				}
				return hm;
			}
		}
	}
}