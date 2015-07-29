package org.scalameter.japi;

import java.io.Serializable;
import org.scalameter.Gen;
import org.scalameter.picklers.*;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;
import scala.collection.immutable.List;
import scala.collection.mutable.ArrayOps;
import scala.runtime.BoxedUnit;


/** Java version of the [[org.scalameter.Gen]].
 *  After combining and changing your generators
 *  you should finally call the `asScala()` method.
 *
 *  Note that it's immutable, so every method return new JGen.
 */
@SuppressWarnings({"unchecked"})
public class JGen<T> implements Serializable {
  private final Gen<T> gen;

  public JGen(Gen<T> gen) {
    this.gen = gen;
  }

  public Gen<T> asScala() {
    return this.gen;
  }

  public JGen<T> cached() {
    return new JGen<T>(gen.cached());
  }

  public <S> JGen<Tuple2<T, S>> zip(JGen<S> that) {
    return new JGen<Tuple2<T, S>>(this.gen.zip(that.gen));
  }

  public <S> JGen<S> map(final Fun1<T, S> f) {
    return new JGen<S>(this.gen.map(new SerializableAbstractFunction1<T, S>() {
      public S apply(T v) {
        return f.apply(v);
      }
    }));
  }


  /* Standard factory methods. */

  public static JGen<Integer> exponential(
      String axisName, int from, int until, int factor
  ) {
    return new JGen<Integer>(
        (Gen<Integer>)(Object) Gen.exponential(axisName, from, until, factor)
    );
  }

  public static JGen<Integer> range(
      String axisName, int from, int upto, int hop
  ) {
    return new JGen<Integer>(
        (Gen<Integer>)(Object) Gen.range(axisName, from, upto, hop)
    );
  }

  public static JGen<BoxedUnit> none(String axisName) {
    return new JGen<BoxedUnit>(Gen.unit(axisName));
  }


  /* Factory methods that produce cross product of multiple JGen instances. */

  public static <P, Q> JGen<Tuple2<P, Q>> crossProduct(
      JGen<P> p, JGen<Q> q
  ) {
    return new JGen<Tuple2<P, Q>>(
        Gen.crossProduct(p.gen, q.gen)
    );
  }

  public static <P, Q, R> JGen<Tuple3<P, Q, R>> crossProduct(
      JGen<P> p, JGen<Q> q, JGen<R> r
  ) {
    return new JGen<Tuple3<P, Q, R>>(
        Gen.crossProduct(p.gen, q.gen, r.gen)
    );
  }

  public static <P, Q, R, S> JGen<Tuple4<P, Q, R, S>> crossProduct(
      JGen<P> p, JGen<Q> q, JGen<R> r, JGen<S> s
  ) {
    return new JGen<Tuple4<P, Q, R, S>>(
        Gen.crossProduct(p.gen, q.gen, r.gen, s.gen)
    );
  }


  /* Factory methods that return enumerations. */

  private static <T> List<T> toList(T[] array) {
    return new ArrayOps.ofRef(array).toList();
  }

  public static JGen<Boolean> booleanValues(
      String axisName, Boolean... values
  ) {
    return new JGen<Boolean>(Gen.enumeration(
        axisName, toList(values), (Pickler<Boolean>)(Object) Implicits.booleanPickler()
    ));
  }

  public static JGen<Character> charValues(
      String axisName, Character... values
  ) {
    return new JGen<Character>(Gen.enumeration(
        axisName, toList(values), (Pickler<Character>)(Object) Implicits.charPickler()
    ));
  }

  public static JGen<Byte> byteValues(
      String axisName, Byte... values
  ) {
    return new JGen<Byte>(Gen.enumeration(
        axisName, toList(values), (Pickler<Byte>)(Object) Implicits.bytePickler()
    ));
  }

  public static JGen<Short> shortValues(
      String axisName, Short... values
  ) {
    return new JGen<Short>(Gen.enumeration(
        axisName, toList(values), (Pickler<Short>)(Object) Implicits.shortPickler()
    ));
  }

  public static JGen<Integer> intValues(
      String axisName, Integer... values
  ) {
    return new JGen<Integer>(Gen.enumeration(
        axisName, toList(values), (Pickler<Integer>)(Object) Implicits.intPickler()
    ));
  }

  public static JGen<Long> longValues(
      String axisName, Long... values
  ) {
    return new JGen<Long>(Gen.enumeration(
        axisName, toList(values), (Pickler<Long>)(Object) Implicits.longPickler()
    ));
  }

  public static JGen<Float> floatValues(
      String axisName, Float... values
  ) {
    return new JGen<Float>(Gen.enumeration(
        axisName, toList(values), (Pickler<Float>)(Object) Implicits.floatPickler()
    ));
  }

  public static JGen<Double> doubleValues(
      String axisName, Double... values
  ) {
    return new JGen<Double>(Gen.enumeration(
        axisName, toList(values), (Pickler<Double>)(Object) Implicits.doublePickler()
    ));
  }

  public static <T extends Enum<T>> JGen<T> enumValues(
      String axisName, T... values
  ) {
    return new JGen<T>(Gen.enumeration(
        axisName, toList(values), (Pickler<T>) Implicits.enumPickler()
    ));
  }

  public static <T> JGen<T> values(
      String axisName, Pickler<T> pickler, T... values
  ) {
    return new JGen<T>(Gen.enumeration(
        axisName, toList(values), pickler
    ));
  }
}
