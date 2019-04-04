package au.com.simplemachines.blob.mem

import java.io.ByteArrayInputStream
import java.nio.channels.{Channels, ReadableByteChannel}
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import au.com.simplemachines.blob._
import com.google.common.cache.CacheBuilder
import com.google.common.io.ByteStreams

/**
  * A simple in-memory [[BlobService]].
  * <p/>
  * The underlying store is a soft-valued concurrent map with fixed write expiry.
  */
class InMemoryBlobService(expiryMinutes: Int) extends AbstractBaseBlobService {

  val counter = new AtomicLong

  val store = CacheBuilder.newBuilder()
    .softValues()
    .expireAfterWrite(expiryMinutes, TimeUnit.MINUTES)
    .build[BKey, (InMemoryBlobDescriptor, Array[Byte])]()

  def allocate(name: String, mimeType: String): BKey = {
    val key = BKey(counter.getAndIncrement.toString)
    store.put(key, (InMemoryBlobDescriptor(name, mimeType, 0), Array()))
    key
  }

  def update(key: BKey, in: ReadableByteChannel, contentLength: Long, mimeType: String): Unit = {
    update(key, in, contentLength, mimeType, null)
  }

  override def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String, cacheControl: String): Unit = {
    val entry = get(key)

    store.put(key, (
      InMemoryBlobDescriptor(entry._1.name, contentType, contentLength),
      ByteStreams.toByteArray(Channels.newInputStream(in))))
  }

  def copy(source: BKey, destination: BKey): Unit = {
    val entry = get(source)

    store.put(destination, entry)
  }

  def read(key: BKey): ReadableByteChannel = Channels.newChannel(new ByteArrayInputStream(get(key)._2))

  def getInfo(key: BKey): BInfo = get(key)._1.toBInfo(key)

  private def get(key: BKey): (InMemoryBlobDescriptor, Array[Byte]) = {
    val entry: (InMemoryBlobDescriptor, Array[Byte]) = store.getIfPresent(key)
    require(entry != null, "Keys must first be allocated. This store doesn't know about your key.")
    entry
  }

  def delete(key: BKey): Unit = {
    store.invalidate(key)
  }

  def listForPrefix(prefix: String, delimiter: String, marker: Option[String], maxKeys: Option[Int]): BListings = {
    ???
  }

  def exists(key: BKey): Boolean = {
    Option(store.getIfPresent(key)).isDefined
  }
}

private[mem] case class InMemoryBlobDescriptor(name: String, mimeType: String, size: Long) {
  def toBInfo(key: BKey): BInfo = BInfo(key, mimeType, name, Instant.now(), size)
}
