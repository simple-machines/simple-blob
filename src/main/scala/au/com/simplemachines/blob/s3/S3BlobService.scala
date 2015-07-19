package au.com.simplemachines.blob.s3

import java.nio.channels.{Channels, ReadableByteChannel}
import java.time.Instant

import au.com.simplemachines.blob._

/** An Amazon S3 implementation of the {@link BlobService} service.
  * </p>
  * The match isn't a perfect fit in-terms of efficiency but its not too bad. */
class S3BlobService(s3Agent: S3Agent, mimeTypeLookup: MimeTypeLookup, bucketName: String)
  extends AbstractBaseBlobService(mimeTypeLookup) {

  /**
   * A convenience constructor that creates a {@link S3AgentImpl} using the provided creds and a {@link UrlMimeTypeLookup}.
   *
   * Region is used for non-US based s3 buckets, ie sydney is: s3-ap-southeast-2
   * It goes to form the url like such: [bucketName].[region].amazonaws.com
   */
  def this(secretKey: String, accessKey: String, bucketName: String, region: String) =
    this(new S3AgentImpl(accessKey, secretKey, region), new UrlMimeTypeLookup(), bucketName)

  /**
   * Convenience constructor that uses the default s3 (US) region.
   */
  def this(secretKey: String, accessKey: String, bucketName: String) = this(secretKey, accessKey, bucketName, "s3")

  override def allocate(name: String, mimeType: String): BKey = {
    // Requests to S3 are idempotent with the last winning. In terms of allocation, the name,
    // if valid (and there aren't any ACL conflicts), is as good as it needs to be.
    new BKey(name)
  }

  def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String) {
    s3Agent.put(bucketName, key.key, contentLength, Channels.newInputStream(in), contentType)
  }

  override def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String, cacheControl: String) {
    s3Agent.put(bucketName, key.key, contentLength, Channels.newInputStream(in), contentType, Option(cacheControl))
  }

  override def read(key: BKey): ReadableByteChannel = {
    val blob = s3Agent.get(bucketName, key.key) getOrElse (
      throw new NullPointerException("No entity at key:" + key))

    Channels.newChannel(blob.content)
  }

  override def getInfo(key: BKey): BInfo = {
    val summary = s3Agent.head(bucketName, key.key) getOrElse (
      throw new NullPointerException("No entity at key:" + key))

    new BInfo(key, summary.contentType, summary.name, Instant.ofEpochMilli(summary.lastModified), summary.size)
  }

}