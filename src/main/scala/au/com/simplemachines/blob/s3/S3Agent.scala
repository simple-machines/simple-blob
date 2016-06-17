package au.com.simplemachines.blob.s3

import java.io.InputStream

/**
 * A simpler interface for dealing with S3 than the official Amazon client.
 **/
trait S3Agent {

  /**
   * PUT the provided <code>InputStream</code> in bucket.
   * <p/>
   * The contentLength is required as S3 needs it explicitly set before sending data; the alternative would be to buffer
   * the input stream and no-one would be happy about that.
   * @see http://docs.amazonwebservices.com/AmazonS3/latest/API/RESTObjectPUT.html
   **/
  def put(bucketName: String, key: String, contentLength: Long, in: InputStream, contentType: String, cacheControl: Option[String] = None)

  /**
    * This implementation of the PUT operation creates a copy of an object that is already stored in Amazon S3.
    * A PUT copy operation is the same as performing a GET and then a PUT.
    */
  def putCopy(bucketName: String, key: String, source: String)

  /**
   * Get the [[S3Object]] at key.
   **/
  def get(bucketName: String, key: String): Option[S3Object]


  /** Get the object summary. */
  def head(bucketName: String, key: String): Option[ObjectMetadata]


  /**
   * List objects in a Bucket.
   *
   * Buckets can contain a virtually unlimited number of keys, and the complete results of a list query can be extremely
   * large. To manage large result sets, S3 uses pagination to split them into multiple responses.
   **/
  def list(bucketName: String,
           prefix: Option[String] = None,
           marker: Option[String] = None,
           delimiter: Option[String] = None,
           maxKeys: Option[String] = None): ObjectListing


  /**
   * Delete the [[S3Object]] at key.
   **/
  def delete(bucketName: String, key: String): Boolean

}