package org.scalameter.examples;



import java.util.HashMap;
import org.scalameter.japi.*;



public class JBeforeAfterTest extends OnlineRegressionReport {
	public class Range implements Group {
		public final JContext config = JContext.create()
		  .put("exec.benchRuns", 30)
		  .put("exec.independentSamples", 3);
		public class ToArray implements org.scalameter.japi.Using<Integer, int[]> {
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
