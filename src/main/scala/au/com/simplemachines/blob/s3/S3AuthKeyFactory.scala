package au.com.simplemachines.blob.s3

import java.util.Base64

import com.google.common.base.Strings

import collection.mutable.ListBuffer
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

object S3AuthKeyFactory extends S3AuthKeyFactory

/**A Factory to create the various values needed to be used as the Authorization header for AWS requests.
  * @see http://docs.amazonwebservices.com/AmazonS3/2006-03-01/dev/RESTAuthentication.html */
trait S3AuthKeyFactory {

  /**The collection of sub resource keys that must be reported (in the order they must be reported in). */
  private val SubResourceKeys = List("acl", "lifecycle", "location", "logging", "notification", "partNumber", "policy",
    "requestPayment", "torrent", "uploadId", "uploads", "versionId", "versioning", "versions", "website")

  private val AmzHeaderPrefix = "x-amz-"

  private val encoder = Base64.getEncoder

  /**The basic idea is that AWS will authentication the request by comparing the hash to that which they can create
    * from the request. So, only values that are included in the request need to be provided.
    *
    * @param accessKey the AWS API access key.
    * @param secretKey the AWS API secret key.
    * @param stringToSign the string to sign with the seccret key.*/
  def makeAuthorization(accessKey: String, secretKey: String, stringToSign: String): String = {
    // Prepare a new MAC encoder for this request.
    val mac = Mac.getInstance("HmacSHA1")
    val secret = new SecretKeySpec(secretKey.getBytes, "HmacSHA1")
    mac.init(secret)

    // Sign the string and base64 encode the result.
    val signature = encoder.encodeToString(mac.doFinal(stringToSign.getBytes)).trim()

    "AWS %s:%s".format(accessKey, signature)
  }

  /**Make the string to sign.
    *
    * @param httpVerb the HTTP verb of the request (GET, POST, etc).
    * @param date the value of the Date header.
    * @param canonicalHeaders the canonical headers of the request.
    * @param canonicalResource the canonical resource of the request.
    * @param contentType the value of the Content-Type header (if present in request).
    * @param contentMd5 the MD5 of any content being sent (if present in request). */
  def makeStringToSign(httpVerb: String, date: String, canonicalResource: String, canonicalHeaders: String = "",
                       contentType: String = "", contentMd5: String = ""): String = {
    require(!Strings.isNullOrEmpty(httpVerb))
    require(!Strings.isNullOrEmpty(date))
    require(canonicalHeaders != null)
    require(!Strings.isNullOrEmpty(canonicalResource))

    val result = new StringBuilder()
      .append(httpVerb.toUpperCase).append("\n")
      .append(contentMd5).append("\n")
      .append(contentType).append("\n")
      .append(date).append("\n")

    if (!Strings.isNullOrEmpty(canonicalHeaders)) {
      result.append(canonicalHeaders).append("\n")

    }
    result.append(canonicalResource)

    result.toString()
  }

  /**Make the canonical resources string.
    *
    * <ul>
    *   <li>Start with the empty string ("").</li>
    *   <li>If the request specifies a bucket using the HTTP Host header (virtual hosted-style), append the bucket name
    *     preceded by a "/" (e.g., "/bucketname"). For path-style requests and requests that don't address a bucket,
    *     do nothing. For more information on virtual hosted-style requests, see Virtual Hosting of Buckets.
    *   <li>Append the path part of the un-decoded HTTP Request-URI, up-to but not including the query string.
    *   <li>If the request addresses a sub-resource, like ?versioning, ?location, ?acl, ?torrent, ?lifecycle, or
    *     ?versionid append the sub-resource, its value if it has one, and the question mark. Note that in case of
    *     multiple sub-resources, sub-resources must be lexicographically sorted by sub-resource name and separated by
    *     '&'. e.g. ?acl&versionId=value.
    *   <li>The list of sub-resources that must be included when constructing the CanonicalizedResource Element are:
    *     acl, lifecycle, location, logging, notification, partNumber, policy, requestPayment, torrent, uploadId,
    *     uploads, versionId, versioning, versions and website.
    *   <li>If the request specifies query string parameters overriding the response header values (see Get Object),
    *     append the query string parameters, and its values. When signing you do not encode these values. However,
    *     when making the request, you must encode these parameter values. The query string parameters in a GET
    *     request include response-content-type, response-content-language, response-expires, response-cache-control,
    *     response-content-disposition, response-content-encoding.
    *   <li>The delete query string parameter must be including when creating the CanonicalizedResource for a
    *     Multi-Object Delete request.
    * </ul>
    *
    * @param bucket the bucket name.
    * @param path: HTTP-Request-URI, from the protocol name up to the query string.
    * @param subResourceQueryString: If present. For example "?acl", "?location", "?logging", or "?torrent"*/
  def makeCanonicalResource(bucket: String, path: String, subResourceQueryString: String): String = {
    require(path.startsWith("/"), "Expected startswith '/' but got: " + path)
    require(Strings.isNullOrEmpty(subResourceQueryString) || subResourceQueryString.startsWith("?"))

    val result = new StringBuilder

    if (!Strings.isNullOrEmpty(bucket)) {
      result.append("/").append(bucket)
    }

    result.append(path)

    if (!Strings.isNullOrEmpty(subResourceQueryString)) {
      val queryParams = QueryParamScanner.parse(subResourceQueryString).toMap
      val paramsToKeep = ListBuffer.empty[String]

      for (subResource <- SubResourceKeys) {
        if (queryParams.contains(subResource)) {
          paramsToKeep += (subResource + "=" + queryParams(subResource))
        }
      }

      if (!paramsToKeep.isEmpty) {
        result.append("?").append(paramsToKeep.mkString("&"))
      }
    }

    result.toString()
  }

  /**
   * Make the canonical headers string.
   *
   * <ul>
   *   <li>Convert each HTTP header name to lower-case. For example, 'X-Amz-Date' becomes 'x-amz-date'.</li>
   *   <li>Sort the collection of headers lexicographically by header name.</li>
   *   <li>Combine header fields with the same name into one "header-name:comma-separated-value-list" pair as prescribed
   *     by RFC 2616, section 4.2, without any white-space between values. For example, the two metadata headers
   *     'x-amz-meta-username: fred' and 'x-amz-meta-username: barney' would be combined into the single header
   *     'x-amz-meta-username: fred,barney'.</li>
   *   <li>"Unfold" long headers that span multiple lines (as allowed by RFC 2616, section 4.2) by replacing the folding
   *     white-space (including new-line) by a single space.</li>
   *   <li>Trim any white-space around the colon in the header. For example, the header 'x-amz-meta-username: fred,barney'
   *     would become 'x-amz-meta-username:fred,barney'</li>
   *   <li>Finally, append a new-line (U+000A) to each canonicalized header in the resulting list. Construct the
   *     CanonicalizedResource element by concatenating all headers in this list into a single string.</li>
   * </ul>
   *
   * @param headers the HTTP request headers included in the AWS request.*/
  def makeCanonicalAmzHeaders(headers: Map[String, List[String]]): String = {
    headers.keys
      .filter(_.toLowerCase.startsWith(AmzHeaderPrefix))
      .toSeq.sorted
      .map {k =>
      k.toLowerCase + ":" + (headers(k).map(_.split('\n').map(_.trim).mkString(" ")).mkString(","))
    }.mkString("\n")
  }

}