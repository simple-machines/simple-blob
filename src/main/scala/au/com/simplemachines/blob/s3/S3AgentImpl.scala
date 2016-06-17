package au.com.simplemachines.blob.s3

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.{Clock, ZoneId, Instant}
import java.time.format.DateTimeFormatter

import au.com.simplemachines.blob.s3.S3AuthKeyFactory._
import au.com.simplemachines.net.{RestMate, RestMateRequestOptions, RestOps}

import scala.xml.XML


/** An implementation of [[S3Agent]] that uses [[RestOps]]. */
class S3AgentImpl(restService: RestOps, accessKey: String, secretKey: String, region: String = "s3")(implicit clock: Clock = Clock.systemUTC()) extends S3Agent {

  def this(accessKey: String, secretKey: String, region: String) = this(new RestMate(), accessKey, secretKey, region)

  import S3AgentImpl._

  override def put(bucketName: String, key: String, contentLength: Long, in: InputStream, contentType: String, cacheControl: Option[String] = None) {
    val response =
      S3HttpRequest(bucketName, pathForKey(key), Credentials(accessKey, secretKey), "PUT", Some(PostData(in, contentLength, contentType, cacheControl.getOrElse("no-cache"))), region)
        .execute(restService)

    if (!response.isSuccessful) {
      throw new RuntimeException("Unsuccessful put of " + key + " got " + response.statusCode + " " + response.statusMessage + "\n" + response.getResponseBodyAsString)
    }
  }

  override def putCopy(bucketName: String, key: String, source: String) = {
    val response =
      S3HttpRequest(bucketName, pathForKey(key), Credentials(accessKey, secretKey), "PUT",
        amzHeaders = Map("x-amz-copy-source" -> List(pathForKey(bucketName + "/" + source))))
        .execute(restService)

    if (!response.isSuccessful) {
      throw new RuntimeException("Unsuccessful put of " + key + " got " + response.statusCode + " " + response.statusMessage + "\n" + response.getResponseBodyAsString)
    }
  }

  // --------------------------------------------------------------------------

  override def get(bucketName: String, key: String): Option[S3Object] = {
    val response = S3HttpRequest(bucketName, pathForKey(key), Credentials(accessKey, secretKey), region = region)
      .execute(restService)

    if (response.isSuccessful) {
      response.responseBody.map(b => S3Object(bucketName, key, new ByteArrayInputStream(b)))
    } else None
  }

  // --------------------------------------------------------------------------

  override def head(bucketName: String, key: String): Option[ObjectMetadata] = {
    val response = S3HttpRequest(bucketName, pathForKey(key), Credentials(accessKey, secretKey), "HEAD", region = region)
      .execute(restService)

    if (response.isSuccessful) {
      val length = response.headers.get("Content-Length").map(_.toInt).getOrElse(0)
      val contentType = response.headers.getOrElse("Content-Type", "")
      val lastModified = parseS3Date(response.headers.getOrElse("Last-Modified", ""))

      Some(ObjectMetadata(bucketName, key, FilenameUtils.getName(key), length, contentType, lastModified))
    } else None
  }

  // --------------------------------------------------------------------------

  override def list(bucketName: String,
                    prefix: Option[String] = None,
                    marker: Option[String] = None,
                    delimiter: Option[String] = None,
                    maxKeys: Option[String] = None): ObjectListing = {

    val queryString = (prefix.map(("prefix", _)) ::marker.map(("marker", _)) ::delimiter.map(("delimiter", _)) ::maxKeys.map(("max-keys", _)) :: Nil)
      .flatten
      .map {
        case (k, v) => k + "=" + v
      }.mkString("&")

    val response = S3HttpRequest(bucketName, "/?" + queryString, Credentials(accessKey, secretKey), region = region)
      .execute(restService)

    translateListBucketResult(response.getResponseBodyAsString.get)
  }

  private def translateListBucketResult(xmlStr: String): ObjectListing = {
    val listingBucketResult = XML.loadString(xmlStr)

    val bucketName = (listingBucketResult \ "Name").text

    ObjectListing(bucketName = bucketName,
      objectSummaries = (listingBucketResult \\ "Contents") map {
        contentsNode =>
          S3ObjectSummary(
            bucketName,
            (contentsNode \ "Key").text,
            (contentsNode \ "Size").text.toInt)
      },
      commonPrefixes = (listingBucketResult \\ "CommonPrefixes") map {
        prefixNode => (prefixNode \ "Prefix").text
      })
  }

  // --------------------------------------------------------------------------

  override def delete(bucketName: String, key: String): Boolean = {
    val response = S3HttpRequest(bucketName, pathForKey(key), Credentials(accessKey, secretKey), "DELETE", region = region)
      .execute(restService)

    response.isSuccessful
  }

  // --------------------------------------------------------------------------

}

private case class Credentials(accessKey: String, secretKey: String)

private[s3] case class PostData(in: InputStream, length: Long, contentType: String, cacheControl: String)

private[s3] case class S3HttpRequest(bucketName: String,
                                     path: String,
                                     credentials: Credentials,
                                     method: String = "GET",
                                     postData: Option[PostData] = None,
                                     region: String = "s3",
                                     amzHeaders: Map[String, List[String]] = Map.empty)(implicit clock: Clock) {

  // The "virtual" host for bucket (i.e. foo.s3.amazonaws.com).
  val host = S3AgentImpl.virtualHostForBucket(bucketName, region)

  // Formatted for AWS date.
  val date = S3AgentImpl.currentDate(clock)

  // The HTTP headers. To be passed to RestMate.
  val headers = {
    val cResource = makeCanonicalResource(bucketName, path.split('?')(0), "")
    val cType = for (pd <- postData) yield pd.contentType
    val cHeaders = makeCanonicalAmzHeaders(amzHeaders)
    val reqStr = makeStringToSign(method, date, cResource, cHeaders, cType.getOrElse(""))

    val builder = collection.mutable.ListBuffer(
      "Authorization" -> makeAuthorization(credentials.accessKey, credentials.secretKey, reqStr),
      "Host" -> host,
      "Date" -> date)

    for (pd <- postData) {
      builder +=
        ("Content-Length" -> pd.length.toString) +=
        ("Content-Type" -> pd.contentType) +=
        ("Cache-Control" -> pd.cacheControl)
    }

    for ((n, vs) <- amzHeaders; v <- vs) {
      builder += n -> v
    }

    builder.toList
  }

  // The RestMate options.
  val opts = RestMateRequestOptions(
    headers = headers,
    body = postData.map(_.in)
  )

  // The URL to actually request.
  val url = "https://" + host + path

  /** Execute the represented request with the provided RestOps. */
  def execute(restService: RestOps) = {

    val response = method match {
      case "GET" => restService.get(url, opts)
      case "PUT" => restService.put(url, opts)
      case "DELETE" => restService.delete(url, opts)
      case "HEAD" => restService.head(url, opts)
    }

    response
  }
}

object S3AgentImpl {
  val S3ApiUrl = "amazonaws.com"

  private[s3] def virtualHostForBucket(bucket: String, region: String) = bucket + "." + region + "." + S3ApiUrl

  private[s3] def pathForKey(key: String) = if (key.startsWith("/")) {
    URLEncoder.encode(key, "UTF-8")
  } else {
    "/" + URLEncoder.encode(key, "UTF-8")
  }

  private[s3] def parseS3Date(dateStr: String): Long = {
    val formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")
    formatter.parse(dateStr).getTime
  }

  private[s3] def currentDate(clock: Clock): String = {
    val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z").withZone(ZoneId.of("UTC"))
    formatter.format(Instant.now(clock))
  }
}