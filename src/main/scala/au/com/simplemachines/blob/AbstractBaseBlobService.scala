package au.com.simplemachines.blob

import java.io.{IOException, ByteArrayInputStream}
import java.nio.ByteBuffer
import java.nio.channels.{ReadableByteChannel, Channels, WritableByteChannel}

import au.com.simplemachines.blob.util.DelegatingWritableByteChannelWrapper

object AbstractBaseBlobService {
  private val DEFAULT_MIME_TYPE: String = "application/octet-stream"
}

abstract class AbstractBaseBlobService(mimeTypeLookup: MimeTypeLookup = new UrlMimeTypeLookup) extends BlobService {

  def allocate(name: String): BKey =
    allocate(name, mimeTypeLookup.resolveMimeTypeForExtension(name, AbstractBaseBlobService.DEFAULT_MIME_TYPE))

  def update(key: BKey): WritableByteChannel = {
    new DelegatingWritableByteChannelWrapper() {
      def handleBuffer(buffer: Array[Byte]) {
        update(key, Channels.newChannel(new ByteArrayInputStream(buffer)), buffer.length)
      }
    }
  }

  def update(key: BKey, in: ReadableByteChannel, contentLength: Long): Unit = {
    val info = getInfo(key)
    update(key, in, contentLength, info.mimeType)
  }

  def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String, cacheControl: String): Unit = {
    throw new UnsupportedOperationException("The blob service you are using does not support setting cache control header, feel free to implement it.")
  }

  @throws(classOf[IOException])
  def readInto(key: BKey, out: WritableByteChannel): Unit = {
    val in = read(key)
    try {
      copy(in, out)
    } catch {
      case ex: IOException => {
        throw new BlobServiceException("While serving (readInto) key: " + key, ex)
      }
    } finally {
      in.close()
      out.close()
    }
  }

  def copy(from: ReadableByteChannel, to: WritableByteChannel): Long = {
    val buf = ByteBuffer.allocate(4096)

    var total = 0L
    while (from.read(buf) != -1) {
      buf.flip()
      while (buf.hasRemaining) {
        total += to.write(buf)
      }
      buf.clear()
    }
    total
  }
}
