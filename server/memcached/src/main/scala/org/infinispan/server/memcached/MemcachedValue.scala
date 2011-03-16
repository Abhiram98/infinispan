package org.infinispan.server.memcached

import org.infinispan.server.core.CacheValue
import org.infinispan.util.Util
import java.io.{ObjectOutput, ObjectInput}
import org.infinispan.marshall.Marshallable

/**
 * Memcached value part of key/value pair containing flags on top the common
 * byte array and version.
 *
 * The class can be marshalled either via its externalizer or via the JVM
 * serialization.  The reason for supporting both methods is to enable
 * third-party libraries to be able to marshall/unmarshall them using standard
 * JVM serialization rules.  The Infinispan marshalling layer will always
 * chose the most performant one, aka the Externalizer method.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
// TODO: putting Ids.MEMCACHED_CACHE_VALUE fails compilation in 2.8 - https://lampsvn.epfl.ch/trac/scala/ticket/2764
@Marshallable(externalizer = classOf[MemcachedValue.Externalizer], id = 56)
@serializable
class MemcachedValue(override val data: Array[Byte], override val version: Long, val flags: Long)
      extends CacheValue(data, version) {

   override def toString = {
      new StringBuilder().append("MemcachedValue").append("{")
         .append("data=").append(Util.printArray(data, false))
         .append(", version=").append(version)
         .append(", flags=").append(flags)
         .append("}").toString
   }

}

object MemcachedValue {
   class Externalizer extends org.infinispan.marshall.Externalizer {
      override def writeObject(output: ObjectOutput, obj: AnyRef) {
         val cacheValue = obj.asInstanceOf[MemcachedValue]
         output.writeInt(cacheValue.data.length)
         output.write(cacheValue.data)
         output.writeLong(cacheValue.version)
         output.writeLong(cacheValue.flags)
      }

      override def readObject(input: ObjectInput): AnyRef = {
         val data = new Array[Byte](input.readInt())
         input.readFully(data)
         val version = input.readLong
         val flags = input.readLong
         new MemcachedValue(data, version, flags)
      }
   }
}