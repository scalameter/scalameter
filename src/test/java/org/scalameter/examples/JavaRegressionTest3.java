package org.scalameter.examples;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.scalameter.OnlineRegressionReport;
import org.scalameter.javaApi.Collections;
import org.scalameter.javaApi.Group;
import org.scalameter.javaApi.JavaGenerator;
import org.scalameter.javaApi.Persistor;
import org.scalameter.javaApi.SerializationPersistor;
import org.scalameter.javaApi.SingleGen;
import org.scalameter.javaApi.exec;

public class JavaRegressionTest3 extends OnlineRegressionReport{
	public Persistor javaPersistor(){
		return new SerializationPersistor();
	}
	public class List implements Group{
		public class groupBy implements org.scalameter.javaApi.Using<LinkedList<Integer>, HashMap<Integer, LinkedList<Integer>>>{
			public HashMap<exec, Object> config(){
				HashMap<exec, Object> config = new HashMap<exec, Object>();
		        config.put(exec.benchRuns, 20);
				config.put(exec.independentSamples, 1);
				config.put(exec.outliersCovMultiplier, 1.5);
				config.put(exec.outliersSuspectPercent, 40);
				return config;
			}
			public JavaGenerator<LinkedList<Integer>> generator() {
				JavaGenerator<Integer> sizes = new SingleGen("size", 5000000);
				return new Collections(sizes).lists();
			}
			public HashMap<Integer, LinkedList<Integer>> snippet(LinkedList<Integer> in) {
				HashMap<Integer, LinkedList<Integer>> hm = new HashMap<Integer, LinkedList<Integer>>();
				for(int i = 0; i < 10; i++){
					hm.put(0, new LinkedList<Integer>());
				}
				Iterator<Integer> it = in.iterator();
				while(it.hasNext()){
					Integer element = it.next();
					LinkedList<Integer> tmp = hm.get(element%10);
					tmp.add(element);
					hm.put(element%10, tmp);
				}
				return hm;
			}
		}
	}
}