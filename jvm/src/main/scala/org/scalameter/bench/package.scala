package org.scalameter
package bench



import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.Pickler


package object bench {

  @deprecated("Please use Bench.LocalTime instead", "0.7")
  type Quickbenchmark = LocalTime
  

  @deprecated("Please use Bench.ForkedTime instead", "0.7")
  type Microbenchmark = ForkedTime
  
}
