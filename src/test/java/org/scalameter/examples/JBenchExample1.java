package org.scalameter.examples;

import java.util.Random;
import org.scalameter.*;
import org.scalameter.japi.*;
import org.scalameter.japi.annotation.*;


@scopes({
    @scopeCtx(scope = "arrays", context = "root")
})
public class JBenchExample1 extends JBench.LocalTime {
  private Random random;

  public final Context root = new ContextBuilder()
      .put("exec.benchRuns", 20)
      .put("exec.maxWarmupRuns", 10)
      .put("exec.independentSamples", 1)
      .build();

  public final JGen<int[]> arrays = JGen.range("size", 10000, 100000, 30000)
      .map(new Fun1<Integer, int[]>() {
        @Override
        public int[] apply(Integer v) {
          return new int[v];
        }
      });

  public void initRandom1() {
    MethodCounter.called("initRandom1");

    random = new Random();
  }

  public void initRandom2() {
    MethodCounter.called("initRandom2");

    random = new Random();
  }

  public void destroyRandom1() {
    MethodCounter.called("destroyRandom1");

    random = null;
  }

  public void destroyRandom2() {
    MethodCounter.called("destroyRandom2");

    random = null;
  }

  public void doNothing1(int[] array) {
    MethodCounter.called("doNothing1");
  }

  public void doNothing2(int[] array) {
    MethodCounter.called("doNothing2");
  }


  public int warmupRandom1() {
    MethodCounter.called("warmupRandom1");

    int result = 0;
    for (int i = 0; i < 100; i++) {
      result += random.nextInt(1000);
    }
    return result;
  }

  public int warmupRandom2() {
    MethodCounter.called("warmupRandom2");

    int result = 0;
    for (int i = 0; i < 100; i++) {
      result += random.nextInt(1000);
    }
    return result;
  }

  public void fillArray1(int[] array) {
    MethodCounter.called("fillArray1");

    for (int i = 0; i < array.length; i++) {
      array[i] = random.nextInt(1000);
    }
  }

  public void fillArray2(int[] array) {
    MethodCounter.called("fillArray2");

    for (int i = 0; i < array.length; i++) {
      array[i] = random.nextInt(1000);
    }
  }


  @gen("arrays")
  @setup("fillArray1")
  @teardown("doNothing1")
  @warmup("warmupRandom1")
  @setupBeforeAll("initRandom1")
  @teardownAfterAll("destroyRandom1")
  @benchmark("arrays.forloops")
  public int sum(int[] array) {
    MethodCounter.called("sum");

    int result = 0;
    for (int i = 0; i < array.length; i++) {
      result += array[i];
    }
    return result;
  }

  @gen("arrays")
  @setup("fillArray2")
  @teardown("doNothing2")
  @warmup("warmupRandom2")
  @setupBeforeAll("initRandom2")
  @teardownAfterAll("destroyRandom2")
  @benchmark("arrays.forloops")
  @curve("multiply")
  public int product(int[] array) {
    MethodCounter.called("product");

    int result = 1;
    for (int i = 0; i < array.length; i++) {
      result *= array[i];
    }
    return result;
  }

  @Override
  public Context defaultConfig() {
    return new ContextBuilder()
        .put("verbose", false)
        .build();
  }

  @Override
  public Warmer warmer() {
    return Warmer.Zero$.MODULE$;
  }
}
