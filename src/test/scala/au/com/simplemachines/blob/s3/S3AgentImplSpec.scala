package au.com.simplemachines.blob.s3

import java.io.ByteArrayInputStream
import java.time.{ZoneId, Instant, Clock}

import au.com.simplemachines.net.{RestMateRequestOptions, RestMateResponse, RestOps}
import com.google.common.io.ByteStreams
import org.specs2.mock.Mockito
import org.specs2.specification.Scope

class S3AgentImplSpec extends org.specs2.mutable.Specification with Mockito {


  "PUT Object" should {

    "fire off a PUT request with correct headers and content to the buckets virtual URL" in new S3AgentImplScope {
      restOps.put(===("https://foo.s3.amazonaws.com/key.txt"), any[RestMateRequestOptions]) returns
        new RestMateResponse(200, "OK", Map("Content-Type" -> "text/plain"), Some("CONTENT".getBytes))

      val requestBody = new ByteArrayInputStream("CONTENT".getBytes)
      agent.put("foo", "key.txt", 100, requestBody, "text/plain")

      there was one(restOps).put(===("https://foo.s3.amazonaws.com/key.txt"), ===(RestMateRequestOptions(
        headers = ("Authorization", "AWS ACCESS_KEY:Y+0MYdP0NarPdinB5uo2Lm9h/Dw=") ::
                  ("Host", "foo.s3.amazonaws.com") ::
                  ("Date", "Mon, 12 Jan 1970 13:46:40 +0000") ::
                  ("Content-Length", "100") ::
                  ("Content-Type", "text/plain") ::
                  ("Cache-Control", "no-cache") ::
                  Nil,
        body = Some(requestBody))))
    }
  }

  "GET Object" should {

    "GET an object and return a corresponding S3Object" in new S3AgentImplScope {
      restOps.get(===("https://foo.s3.amazonaws.com/key.txt"), any[RestMateRequestOptions]) returns
          RestMateResponse(200, "OK", Map("Content-Type" -> "binary/octet-stream"), Some("CONTENT".getBytes))

      agent.get("foo", "key.txt") must beSome
    }

    "Return None if object isn't found." in new S3AgentImplScope {
      restOps.get(===("https://foo.s3.amazonaws.com/key.txt"), any[RestMateRequestOptions]) returns
          RestMateResponse(404, "Not found", Map.empty, None)

      agent.get("foo", "key.txt") must beNone
    }

    "URL encode path" in new S3AgentImplScope {
      restOps.get(===("https://foo.s3.amazonaws.com/key+foo.txt"), any[RestMateRequestOptions]) returns
          RestMateResponse(200, "OK", Map("Content-Type" -> "binary/octet-stream"), Some("".getBytes))

      agent.get("foo", "key foo.txt") must beSome
    }
  }

  "List objects in bucket" should {

    "translate object listing" in new S3AgentImplScope {
      restOps.get(===("https://sm-int-test.s3.amazonaws.com/?"), any[RestMateRequestOptions]) returns
          RestMateResponse(200, "OK", Map("Content-Type" -> "application/xml"),
            Some(classPathResource("/s3/list-response-1.xml")))

      val listing = agent.list("sm-int-test")

      listing.objectSummaries must contain(S3ObjectSummary("sm-int-test", "S3AgentIntegrationSpec.txt", 20)).exactly(1)
    }
  }

  "Delete object" should {

    "delete an object" in new S3AgentImplScope {
      restOps.delete(===("https://dt-int-test.s3.amazonaws.com/key.txt"), any[RestMateRequestOptions]) returns
          RestMateResponse(200, "OK", Map.empty, None)

      agent.delete("dt-int-test", "key.txt") mustEqual (true)
    }
  }

  private trait S3AgentImplScope extends Scope {
    val restOps = mock[RestOps]
    implicit val clock = Clock.fixed(Instant.ofEpochMilli(1000000000), ZoneId.of("UTC"))
    val agent = new S3AgentImpl(restOps, "ACCESS_KEY", "SECRET_KEY")
  }

  private def classPathResource(path: String) = {
    ByteStreams.toByteArray(classOf[S3AgentImpl].getResourceAsStream(path))
  }

}
