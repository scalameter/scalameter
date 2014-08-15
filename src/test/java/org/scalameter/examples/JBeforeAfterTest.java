package org.scalameter.examples;
import java.util.HashMap;

import org.scalameter.OnlineRegressionReport;
import org.scalameter.javaApi.*;


public class JBeforeAfterTest extends OnlineRegressionReport{
	public class Range implements Group{
		public HashMap<exec, Object> config(){
			HashMap<exec, Object> config = new HashMap<exec, Object>();
			config.put(exec.benchRuns, 30);
			config.put(exec.independentSamples, 3);
			return config;
		}
		public class ToArray implements org.scalameter.javaApi.Using<Integer, int[]>{
			public JavaGenerator<Integer> generator() {
				return new RangeGen("size", 1000000, 5000000, 2000000);
			}
			public void beforeTests(){
				System.out.println("ABOUT TO START TESTS!");
			}
			public void afterTests(){
				System.out.println("ALL RANGE TESTS COMPLETED!");
			}
			
			public int[] snippet(Integer in){
				int[] array = new int[in];
				for(int i = 0; i < in; i++) {
					array[i] = i;
				}
				return array;
			}
		}
	}
}
