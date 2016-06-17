package au.com.simplemachines.blob

import java.io.IOException
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

/**
 * A simple non-blocking Blob store.
 * <p/>
 * For simplicity, write is a locking operation and read is not.
 */
object BlobService {
  val CACHE_FOREVER: String = "max-age=31556926"
  val CACHE_REVALIDATE: String = "no-cache"
  val CACHE_NEVER: String = "no-store"
}

trait BlobService {
  /**
   * Allocate a new blob for the provided name and provide a BKey for accessing it.
   * <p/>
   * The mimeType is decided through the extension of the file. If no extension, or the extension is unknown, it'll be
   * application/octet-stream.
   *
   * @param name the name.
   * @return a BKey.
   */
  def allocate(name: String): BKey

  /**
   * Allocate a new blob and provide a BKey for reading/writing to a Blob.
   * <p/>
   * This method only need guarantee that the key is allocated.
   *
   * @param name     the name of the Blob.
   * @param mimeType the MIME Content-Type, or extension, of the blob to be written.
   * @return a BKey for writing/reading.
   */
  def allocate(name: String, mimeType: String): BKey

  /**
   * Get a NIO handle to update bytes to a Blob. Implementors need only commit the [[WritableByteChannel]] on
   * [[java.nio.channels.WritableByteChannel#close()]] (but may choose not to).
   * <p/>
   * <strong>Note: using this method will generally require that the contents written to the blob be buffered first;
   * this may not be a problem, but, if it is, favour providing the [[ReadableByteChannel]] and using
   * [[ReadableByteChannel#update(BKey, java.nio.channels.ReadableByteChannel, long)]]</strong>
   *
   * @param key the key of the Blob.
   * @return a channel to write to.
   */
  def update(key: BKey): WritableByteChannel

  /**
   * Update the blob at provided key with in. The content-type is resolved from the key.
   *
   * @param key           the key of the blob.
   * @param in            the channel to read from.
   * @param contentLength the length of in.
   */
  def update(key: BKey, in: ReadableByteChannel, contentLength: Long)

  /**
   * Update the blob at provided key with in.
   *
   * @param key           the key of the blob.
   * @param in            the channel to read from.
   * @param contentLength the length of in.
   * @param contentType   the content type.
   */
  def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String)

  /**
   * Update the blob at provided key with in.
   *
   * @param key           the key of the blob.
   * @param in            the channel to read from.
   * @param contentLength the length of in.
   * @param contentType   the content type.
   * @param cacheControl  the cache control header to set
   */
  def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String, cacheControl: String)

  /**
    * Copy a blob.
    * @param source the key of the source blob.
    * @param destination the key of the destination blob.
    */
  def copy(source: BKey, destination: BKey)

  /**
   * Get a NIO handle to read bytes from a Blob.
   *
   * @param key the key of the Blob.
   * @return a channel to read from.
   */
  def read(key: BKey): ReadableByteChannel

  /**
   * Write blob at key to out.
   *
   * @param key the key to serve.
   * @param out the channel to write into.
   */
  @throws(classOf[IOException])
  def readInto(key: BKey, out: WritableByteChannel)

  /**
   * Get {@link BInfo} for a Blob.
   *
   * @param key the { @link BKey} of the Blob.
   * @return the Binfo for a Blob.
   */
  def getInfo(key: BKey): BInfo
}
