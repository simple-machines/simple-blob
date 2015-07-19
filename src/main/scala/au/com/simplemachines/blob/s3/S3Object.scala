package au.com.simplemachines.blob.s3

import java.io.InputStream

import com.google.common.io.ByteStreams


/**
 * An object stored in Amazon S3.
 *
 * @param bucketName The name of the Amazon S3 bucket in which this object is stored.
 * @param key The key under which this object is stored in Amazon S3.
 * @param content An input stream containing the contents of this object.
 **/

case class S3Object(bucketName: String, key: String, content: InputStream) {
  def contentAsString: String = new String(ByteStreams.toByteArray(content))
}