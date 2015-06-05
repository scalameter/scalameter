package org.scalameter
package json

import scala.language.higherKinds
import com.fasterxml.jackson.core.{JsonToken, JsonParser, JsonGenerator}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.{SimpleSerializers, SimpleModule}
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.Module.SetupContext
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import scala.collection.generic.ImmutableMapFactory
import scala.collection.immutable


object ConfigKeySerializer extends StdSerializer[Key[_]](classOf[Key[_]]) {
  def serialize(value: Key[_], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeFieldName(value.fullname)
  }
}

object ParameterKeySerializer extends StdSerializer[Parameter[_]](classOf[Parameter[_]]) {
  def serialize(value: Parameter[_], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeFieldName(s"${value.name}|${value.typeHint.toString()}")
  }
}

class TypeHintedMapDeserializer[K <: TypeHintedKey[_], T[_K, +V] <: immutable.Map[_K, V] with immutable.MapLike[_K, V, T[_K, V]]]
(factory: ImmutableMapFactory[T], clazz: Class[T[K, Any]], keyCreator: String => K)
  extends StdDeserializer[T[K, Any]](clazz) {
  def deserialize(p: JsonParser, ctx: DeserializationContext): T[K, Any] = {
    if (p.getCurrentToken != JsonToken.START_OBJECT) throw ctx.mappingException(clazz)

    val builder = factory.newBuilder[K, Any]
    while (p.nextToken() == JsonToken.FIELD_NAME) {
      val key = keyCreator(p.getCurrentName)
      val vD = ctx.findContextualValueDeserializer(mapper.constructType(key.typeHint), null)
      val value = p.nextToken match {
        case JsonToken.VALUE_NULL => vD.getNullValue
        case _ => vD.deserialize(p, ctx)
      }
      builder += (key -> value)
    }

    builder.result()
  }
}

object TypeHintedMapDeserializerResolver extends Deserializers.Base {
  private val allowedTypes: Map[Class[_], JsonDeserializer[_]] = Map(
    classOf[Key[_]] -> new TypeHintedMapDeserializer[Key[_], immutable.Map](
      immutable.Map, classOf[immutable.Map[Key[_], Any]], Key.parseKey).asInstanceOf[JsonDeserializer[_]],
    classOf[Parameter[_]] -> new TypeHintedMapDeserializer[Parameter[_], immutable.ListMap](
      immutable.ListMap, classOf[immutable.ListMap[Parameter[_], Any]], Parameter.fromString).asInstanceOf[JsonDeserializer[_]]
  )

  override def findMapLikeDeserializer(tpe: MapLikeType, 
                                       config: DeserializationConfig, 
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer, 
                                       elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val keyClazz = tpe.getKeyType.getRawClass
    if (classOf[immutable.Map[_, _]].isAssignableFrom(tpe.getRawClass) && allowedTypes.contains(keyClazz)) allowedTypes(keyClazz)
    else null
  }
}

object ScalaMeterModule extends SimpleModule {
  override def setupModule(context: SetupContext): Unit = {
    val keySerializers = new SimpleSerializers()
    keySerializers.addSerializer(classOf[Key[_]], ConfigKeySerializer)
    keySerializers.addSerializer(classOf[Parameter[_]], ParameterKeySerializer)
    context.addKeySerializers(keySerializers)
    context.addDeserializers(TypeHintedMapDeserializerResolver)
  }
}
