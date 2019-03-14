package au.com.simplemachines.blob.s3

import java.nio.channels.{Channels, ReadableByteChannel}

import au.com.simplemachines.blob._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectMetadata => ObjectMetadataAWS}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/** An Amazon S3 Synchronous implementation of the {@link BlobService} service.
  * It uses the standard AWS client.
  *
  * */
class S3SyncBlobService(secretKey: String, accessKey: String, bucketName: String, region: String)
  extends AbstractBaseBlobService {

  val s3Client: AmazonS3 = AmazonS3ClientBuilder
    .standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
    .withRegion(region)
    .build()

  def allocate(name: String, mimeType: String): BKey = {
    // Requests to S3 are idempotent with the last winning. In terms of allocation, the name,
    // if valid (and there aren't any ACL conflicts), is as good as it needs to be.
    BKey(name)
  }

  def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String): Unit = {
    val metadata = new ObjectMetadataAWS()
    metadata.setContentLength(contentLength)
    metadata.setContentType(contentType)
    s3Client.putObject(bucketName, key.key, Channels.newInputStream(in), metadata)
  }

  override def update(key: BKey, in: ReadableByteChannel, contentLength: Long, contentType: String, cacheControl: String): Unit = {
    val metadata = new ObjectMetadataAWS()
    metadata.setContentLength(contentLength)
    metadata.setContentType(contentType)
    metadata.setCacheControl(cacheControl)
    s3Client.putObject(bucketName, key.key, Channels.newInputStream(in), metadata)
  }

  def copy(source: BKey, destination: BKey): Unit = {
    s3Client.copyObject(bucketName, source.key, bucketName, destination.key)
  }

  def read(key: BKey): ReadableByteChannel = {
    Try {
      s3Client.getObject(bucketName, key.key)
    } match {
      case Success(blob) => Channels.newChannel(blob.getObjectContent)
      case Failure(_) =>
        throw new NullPointerException(s"No entity at key: $key")
    }
  }

  def getInfo(key: BKey): BInfo = {
    Try(s3Client.getObjectMetadata(bucketName, key.key)) match {
      case Success(metadata) =>
        BInfo(key, metadata.getContentType, key.key, metadata.getLastModified.toInstant, metadata.getContentLength)
      case Failure(_) => throw new NullPointerException(s"No entity at key: $key")
    }
  }

  def delete(key: BKey): Unit = {
    s3Client.deleteObject(bucketName, key.key)
  }

  def listForPrefix(prefix: String,
                    delimiter: String,
                    marker: Option[String],
                    maxKeys: Option[Int]): BListings = {
    val req = new ListObjectsRequest(
      bucketName,
      prefix,
      marker.orNull,
      delimiter,
      maxKeys.map(x => new java.lang.Integer(x)).orNull)

    val res = s3Client.listObjects(req)
    val prefixes = res.getCommonPrefixes.asScala
    val summaries = res.getObjectSummaries.asScala.map(x => BSummary(x.getKey, x.getSize, x.getLastModified.toInstant))

    BListings(summaries, prefixes)
  }

  def exists(key: BKey): Boolean = {
    s3Client.doesObjectExist(bucketName, key.key)
  }
}

