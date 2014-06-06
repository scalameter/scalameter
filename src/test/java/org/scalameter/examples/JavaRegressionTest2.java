package org.scalameter.examples;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import org.scalameter.OnlineRegressionReport;
import org.scalameter.javaApi.Collections;
import org.scalameter.javaApi.Group;
import org.scalameter.javaApi.JavaGenerator;
import org.scalameter.javaApi.RangeGen;
import org.scalameter.javaApi.exec;

public class JavaRegressionTest2 extends OnlineRegressionReport{
	public class List implements Group{
		public class map implements org.scalameter.javaApi.Using<LinkedList<Integer>, LinkedList<Integer>>{
			public HashMap<exec, Object> config(){
				HashMap<exec, Object> config = new HashMap<exec, Object>();
				config.put(exec.independentSamples, 6);
				return config;
			}
			public JavaGenerator<LinkedList<Integer>> generator() {
				JavaGenerator<Integer> sizes = new RangeGen("size", 1000000, 2000000, 500000);
				return new Collections(sizes).lists();
			}
			public LinkedList<Integer> snippet(LinkedList<Integer> in){
				ListIterator<Integer> it = in.listIterator();
				while(it.hasNext()){
					Integer i = it.next();
					it.set(i+1);;
				}
				return in;
			}
		}
	}
}