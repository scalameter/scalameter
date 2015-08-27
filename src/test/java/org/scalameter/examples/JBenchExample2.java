package org.scalameter.examples;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import org.scalameter.*;
import org.scalameter.japi.*;
import org.scalameter.japi.annotation.*;
import scala.Tuple2;


public class JBenchExample2 extends JBench.OfflineReport {
  public final JGen<Integer> sizes = JGen.range("size", 1000, 25000, 4000);

  public final JGen<Tuple2<Integer, ArrayList<Integer>>> arrayLists = sizes.zip(
      sizes.map(new Fun1<Integer, ArrayList<Integer>>() {
        public ArrayList<Integer> apply(Integer v) {
          return new ArrayList<Integer>();
        }
      })
  );

  public final JGen<Tuple2<Integer, LinkedList<Integer>>> linkedLists = sizes.zip(
      sizes.map(new Fun1<Integer, LinkedList<Integer>>() {
        public LinkedList<Integer> apply(Integer v) {
          return new LinkedList<Integer>();
        }
      })
  );

  public void arrayListSetup(Tuple2<Integer, ArrayList<Integer>> v) {
    int size = v._1();
    ArrayList<Integer> list = v._2();

    Random random = new Random(size);
    for (int i = 0; i < size; i++) {
      list.add(random.nextInt());
    }
  }

  public void linkedListSetup(Tuple2<Integer, LinkedList<Integer>> v) {
    int size = v._1();
    LinkedList<Integer> list = v._2();

    Random random = new Random(size);
    for (int i = 0; i < size; i++) {
      list.add(random.nextInt());
    }
  }

  @Override
  public Context defaultConfig() {
    return new ContextBuilder()
        .put("exec.benchRuns", 25)
        .put("exec.independentSamples", 1)
        .build();
  }

  @gen("arrayLists")
  @setup("arrayListSetup")
  @benchmark("lists.forloops")
  @curve("array")
  public int arrayListSum(Tuple2<Integer, ArrayList<Integer>> v) {
    ArrayList<Integer> list = v._2();
    int result = 0;

    for (Integer elem : list) {
      result += elem;

    }
    return result;
  }

  @gen("linkedLists")
  @setup("linkedListSetup")
  @benchmark("lists.forloops")
  @curve("linked")
  public int linkedListSum(Tuple2<Integer, LinkedList<Integer>> v) {
    LinkedList<Integer> list = v._2();
    int result = 0;

    for (Integer elem : list) {
      result += elem;

    }
    return result;
  }

  @gen("arrayLists")
  @benchmark("lists.ops.add")
  @curve("array")
  public ArrayList<Integer> arrayListAdd(Tuple2<Integer, ArrayList<Integer>> v) {
    int size = v._1();
    ArrayList<Integer> list = v._2();

    Random random = new Random(size);
    for (int i = 0; i < size; i++) {
      list.add(random.nextInt());
    }

    return list;
  }

  @gen("linkedLists")
  @benchmark("lists.ops.add")
  @curve("linked")
  public LinkedList<Integer> linkedListAdd(Tuple2<Integer, LinkedList<Integer>> v) {
    int size = v._1();
    LinkedList<Integer> list = v._2();

    Random random = new Random(size);
    for (int i = 0; i < size; i++) {
      list.add(random.nextInt());
    }

    return list;
  }

  @gen("arrayLists")
  @setup("arrayListSetup")
  @benchmark("lists.ops.remove")
  @curve("array")
  public ArrayList<Integer> arrayListRemove(Tuple2<Integer, ArrayList<Integer>> v) {
    int size = v._1();
    ArrayList<Integer> list = v._2();

    Random random = new Random(size);
    for (int i = 0; i < size; i++) {
      list.remove((Integer) random.nextInt());
    }

    return list;
  }

  @gen("linkedLists")
  @setup("linkedListSetup")
  @benchmark("lists.ops.remove")
  @curve("linked")
  public LinkedList<Integer> linkedListRemove(Tuple2<Integer, LinkedList<Integer>> v) {
    int size = v._1();
    LinkedList<Integer> list = v._2();

    Random random = new Random(size);
    for (int i = 0; i < size; i++) {
      list.remove((Integer) random.nextInt());
    }

    return list;
  }
}
