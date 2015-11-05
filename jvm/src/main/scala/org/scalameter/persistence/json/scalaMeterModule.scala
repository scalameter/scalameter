package org.scalameter
package persistence.json

import com.fasterxml.jackson.core.{JsonToken, JsonParser, JsonGenerator}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.{TypeSerializer, TypeDeserializer}
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.`type`.MapLikeType
import org.scalameter.picklers.Pickler
import scala.collection.immutable


object MeasurementSerializer extends StdSerializer[Measurement[_]](classOf[Measurement[_]]) {
  def serialize(value: Measurement[_], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    val pickler = value.pickler.asInstanceOf[Pickler[Any]]

    jgen.writeStartObject()

    jgen.writeStringField("@pickler", value.pickler.getClass.getName)

    jgen.writeBinaryField("value", pickler.pickle(value.value))

    jgen.writeFieldName("params")
    provider.findValueSerializer(classOf[Parameters]).serialize(value.params, jgen, provider)

    jgen.writeObjectFieldStart("data")
    jgen.writeArrayFieldStart("complete")
    value.data.complete.foreach(v => jgen.writeBinary(pickler.pickle(v)))
    jgen.writeEndArray()
    jgen.writeBooleanField("success", value.data.success)
    jgen.writeEndObject()

    jgen.writeStringField("units", value.units)

    jgen.writeEndObject()
  }
}

object MeasurementDeserializer extends StdDeserializer[Measurement[_]](
  classOf[Measurement[_]]) {
  def deserialize(p: JsonParser, ctx: DeserializationContext): Measurement[_] = {
    def getField(name: String): Option[JsonToken] = {
      Option(p.nextToken()).filter(_ == JsonToken.FIELD_NAME && p.getCurrentName == name)
    }

    val clazz = classOf[Measurement[_]]
    val measurement = for {
      s <- Option(p.getCurrentToken) if s == JsonToken.START_OBJECT

      _ <- getField("@pickler")
      pickler <- Option {
        p.nextToken()
        Pickler.makeInstance[Any](Class.forName(p.getValueAsString))
      }

      _ <- getField("value")
      value <- Option {
        p.nextToken()
        pickler.unpickle(p.getBinaryValue)
      }

      _ <- getField("params")
      params <- Option {
        p.nextToken()
        val paramsType = ctx.getTypeFactory.constructType(classOf[Parameters])
        ctx.findContextualValueDeserializer(paramsType, null)
          .deserialize(p, ctx).asInstanceOf[Parameters]
      }

      _ <- getField("data")
      ds <- Option(p.nextToken()) if ds == JsonToken.START_OBJECT
      _ <- getField("complete")
      complete <- Option {
        val builder = Seq.newBuilder[Any]
        if (p.nextToken() != JsonToken.START_ARRAY) throw ctx.mappingException(clazz)
        p.nextToken()
        while (p.getCurrentToken != JsonToken.END_ARRAY && p.getCurrentToken != null) {
          builder += pickler.unpickle(p.getBinaryValue)
          p.nextToken()
        }
        if (p.getCurrentToken != JsonToken.END_ARRAY) throw ctx.mappingException(clazz)
        builder.result()
      }
      _ <- getField("success")
      success <- Option {
        p.nextToken()
        p.getBooleanValue
      }
      de <- Option(p.nextToken()) if de == JsonToken.END_OBJECT

      _ <- getField("units")
      units <- Option {
        p.nextToken()
        p.getValueAsString
      }

      e <- Option(p.nextToken()) if e == JsonToken.END_OBJECT
    } yield Measurement(value, params, MeasurementData(complete, success), units)(pickler)

    measurement.getOrElse(throw ctx.mappingException(clazz))
  }
}

/** Serializer for maps with keys whose are subtype of [[org.scalameter.PicklerBasedKey]].
 *
 *  It serializes map values as Base64 encoded byte arrays.
 *
 * @tparam MapKey subtype of [[org.scalameter.PicklerBasedKey]]
 * @param clazz handled type
 */
class PicklerBasedMapSerializer[MapKey <: PicklerBasedKey[_]](clazz: Class[immutable.Map[MapKey, Any]])
  extends StdSerializer[immutable.Map[MapKey, Any]](clazz) {
  def serialize(value: immutable.Map[MapKey, Any], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeStartObject()
    value.foreach { case (k, v) =>
      if (!k.isTransient) {
        jgen.writeFieldName(k.repr)
        jgen.writeBinary(k.pickler.asInstanceOf[Pickler[Any]].pickle(v))
      }
    }
    jgen.writeEndObject()
  }
}

/** Deserializer for maps with keys whose are subtype of [[org.scalameter.PicklerBasedKey]].
 *
 * @tparam MapKey subtype of a [[org.scalameter.PicklerBasedKey]]
 * @param clazz handled type
 * @param keyCreator function used to create a concrete [[MapKey]]
 *                   from `fullName` and [[org.scalameter.picklers.Pickler]]
 */
class PicklerBasedMapDeserializer[MapKey <: PicklerBasedKey[_]](
  clazz: Class[immutable.Map[MapKey, Any]], keyCreator: (String, Pickler[_]) => MapKey)
  extends StdDeserializer[immutable.Map[MapKey, Any]](clazz) {
  def deserialize(p: JsonParser, ctx: DeserializationContext): immutable.Map[MapKey, Any] = {
    if (p.getCurrentToken != JsonToken.START_OBJECT) throw ctx.mappingException(clazz)

    val builder = immutable.Map.newBuilder[MapKey, Any]
    while (p.nextToken() == JsonToken.FIELD_NAME) {

      val key = PicklerBasedKey.fromString(p.getCurrentName, keyCreator)
      val pickler = key.pickler
      val value = p.nextToken match {
        case JsonToken.VALUE_STRING => pickler.unpickle(p.getBinaryValue)
        case _ => throw ctx.mappingException(clazz)
      }
      builder += (key -> value)
    }

    builder.result()
  }
}

/** Serializer resolver used for precise selection of a PicklerBasedMapSerializer.
 */
object PicklerBasedMapSerializerResolver extends Serializers.Base {
  private val allowedTypes: Map[Class[_], JsonSerializer[_]] = Map(
    classOf[Key[_]] -> new PicklerBasedMapSerializer[Key[_]](
      classOf[immutable.Map[Key[_], Any]]).asInstanceOf[JsonSerializer[_]],
    classOf[Parameter[_]] -> new PicklerBasedMapSerializer[Parameter[_]](
      classOf[immutable.Map[Parameter[_], Any]]).asInstanceOf[JsonSerializer[_]]
  )

  override def findMapLikeSerializer(config: SerializationConfig,
                                     tpe: MapLikeType,
                                     beanDesc: BeanDescription,
                                     keySerializer: JsonSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {
    val keyClazz = tpe.getKeyType.getRawClass
    if (classOf[immutable.Map[_, _]].isAssignableFrom(tpe.getRawClass) &&
      allowedTypes.contains(keyClazz)) allowedTypes(keyClazz)
    else null
  }
}

/** Deserializer resolver used for precise selection of a PicklerBasedMapDeserializer.
 */
object PicklerBasedMapDeserializerResolver extends Deserializers.Base {
  private val allowedTypes: Map[Class[_], JsonDeserializer[_]] = Map(
    classOf[Key[_]] -> new PicklerBasedMapDeserializer[Key[_]](
      // we ignore pickler for Key since it's reconstructed using key registry
      classOf[immutable.Map[Key[_], Any]], (name, _) => Key.parseKey(name)).asInstanceOf[JsonDeserializer[_]],
    classOf[Parameter[_]] -> new PicklerBasedMapDeserializer[Parameter[_]](
      classOf[immutable.Map[Parameter[_], Any]], (name, pickler) => Parameter(name)(pickler)).asInstanceOf[JsonDeserializer[_]]
  )

  override def findMapLikeDeserializer(tpe: MapLikeType,
                                       config: DeserializationConfig,
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer,
                                       elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val keyClazz = tpe.getKeyType.getRawClass
    if (classOf[immutable.Map[_, _]].isAssignableFrom(tpe.getRawClass) &&
      allowedTypes.contains(keyClazz)) allowedTypes(keyClazz)
    else null
  }
}

object ScalaMeterModule extends SimpleModule {
  addSerializer(classOf[Measurement[_]], MeasurementSerializer)
  addDeserializer(classOf[Measurement[_]], MeasurementDeserializer)

  override def setupModule(context: SetupContext): Unit = {
    context.addSerializers(PicklerBasedMapSerializerResolver)
    context.addDeserializers(PicklerBasedMapDeserializerResolver)
    super.setupModule(context)
  }
}
