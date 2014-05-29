package org.scalameter






class MeasureBuilder(val config: Context) {

  def config(kvs: KeyValue*): MeasureBuilder = new MeasureBuilder(config ++ Context(kvs: _*))

  // def withWarmer

  // def setUp

  // def tearDown

  // def measure

}

