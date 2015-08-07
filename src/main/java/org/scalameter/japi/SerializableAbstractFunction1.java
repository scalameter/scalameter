package org.scalameter.japi;

import java.io.Serializable;
import scala.runtime.AbstractFunction1;


abstract class SerializableAbstractFunction1<T, S>
    extends AbstractFunction1<T, S> implements Serializable {
}
