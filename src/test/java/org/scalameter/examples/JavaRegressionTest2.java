package org.scalameter.examples;



import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import org.scalameter.japi.*;



public class JavaRegressionTest2 extends OnlineRegressionReport {
	public class List implements Group {
		public class map implements org.scalameter.japi.Using<LinkedList<Integer>, LinkedList<Integer>> {
			public final JContext config = JContext.create().put("exec.independentSamples", 6);
			public JavaGenerator<LinkedList<Integer>> generator() {
				JavaGenerator<Integer> sizes = new RangeGen("size", 1000000, 2000000, 500000);
				return new CollectionGenerators(sizes).lists();
			}
			public LinkedList<Integer> snippet(LinkedList<Integer> in) {
				ListIterator<Integer> it = in.listIterator();
				while(it.hasNext()) {
					Integer i = it.next();
					it.set(i+1);;
				}
				return in;
			}
		}
	}
}