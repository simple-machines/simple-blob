package au.com.simplemachines.blob.util

import java.io.{IOException, ByteArrayOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.{Channels, WritableByteChannel}

/**
 * A wrapper around a [[WritableByteChannel]] that provides a template method to handle the input
 * after #close.
 */
abstract class DelegatingWritableByteChannelWrapper extends WritableByteChannel {
  private final val bos: ByteArrayOutputStream = new ByteArrayOutputStream
  private final val cBuffer: WritableByteChannel = Channels.newChannel(bos)

  @throws(classOf[IOException])
  def write(src: ByteBuffer): Int = {
    return cBuffer.write(src)
  }

  def isOpen: Boolean = {
    return cBuffer.isOpen
  }

  @throws(classOf[IOException])
  def close {
    cBuffer.close
    handleBuffer(bos.toByteArray)
  }

  def handleBuffer(buffer: Array[Byte])
}
