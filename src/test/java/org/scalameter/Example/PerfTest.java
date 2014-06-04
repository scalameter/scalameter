package org.scalameter.Example;
import java.math.BigInteger;
import java.util.HashMap;

import org.scalameter.QuickBenchmark;
import org.scalameter.javaApi.*;


public class PerfTest extends QuickBenchmark{

	public class Test implements Group{
		public HashMap<exec, Object> config(){
			HashMap<exec, Object> config = new HashMap<exec, Object>();
			config.put(exec.benchRuns, 1.0);
			return config;
		}
		public class factorial implements org.scalameter.javaApi.Using<Void, Void>{
			public void beforeTests(){
				System.out.println("BEFORE TESTS");
			}
			public void afterTests(){
				System.out.println("AFTER TESTS");
			}
		/*	public void setup(Tuple2<Integer, Integer> i){
				System.out.println("SETUP " + i);
			}
			public void teardown(Tuple2<Integer, Integer> i){
				System.out.println("TEARDOWN " + i);
			}*/
//	    setup => setp = Some((v: Object) => {m.invoke(instance, v)})
//	    teardown
			public JavaGenerator<Void> generator(){
				
				
				JavaGenerator<Integer> g = new RangeGen("Range", 1, 21, 4);
				JavaGenerator<Integer> g2 = new RangeGen("Range2", 2, 40, 7);
				TupledGen<Integer, Integer> t = new TupledGen<Integer, Integer>(g, g2);
				Collections c = new Collections(g);
				VoidGen u = new VoidGen("Unit");
//				JavaGenerator<Integer> g2= new SingleGen<Integer>("Single", 20);
				
//				return new TupledGen<>(g, g2); //TODO see why it is accepted as JavaGenerator<Integer>
			/*	MapFunction f = new MapFunction<Integer, Integer>(){
					public Integer map(Integer i) {
						return i*2;
					}
				};
				JavaGenerator<Integer> gen = g.map(f);*/
				return u;
			}
			public Void snippet(Void in) {
				BigInteger fac = BigInteger.ONE;
				
				for(int i = 1; i < 5; i++){
					fac = fac.multiply(new BigInteger(new Integer(i).toString()));
				}
				System.out.println("Factorial of " + 5 + " is " + fac);
				return null;
			}
	}
}
}