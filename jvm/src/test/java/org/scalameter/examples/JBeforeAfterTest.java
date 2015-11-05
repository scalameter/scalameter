package org.scalameter.examples;



import org.scalameter.deprecatedjapi.*;



public class JBeforeAfterTest extends OfflineReport {
	public class Range implements Group {
		public final JContext config = JContext.create()
		  .put("exec.benchRuns", 30)
		  .put("exec.independentSamples", 1);
		public class ToArray implements org.scalameter.deprecatedjapi.Using<Integer, int[]> {
			public JavaGenerator<Integer> generator() {
				return new RangeGen("size", 100000, 500000, 200000);
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
