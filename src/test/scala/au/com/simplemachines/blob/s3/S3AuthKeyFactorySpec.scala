package au.com.simplemachines.blob.s3

import S3AuthKeyFactory._

class S3AuthKeyFactorySpec extends org.specs2.mutable.Specification {

  // Examples at: http://docs.amazonwebservices.com/AmazonS3/2006-03-01/dev/RESTAuthentication.html
  "S3AuthFactory" should {

    val accessKey = "0PN5J17HBGZHT7JJ3X82"
    val secretKey = "uV3F3YluFJax1cknvbcGwgjvx4QpvB+leU8dUj2o"

    "Support the first AWS example." in {
      val res = makeCanonicalResource("johnsmith", "/photos/puppy.jpg", "")
      val stringToSign = makeStringToSign("GET", "Tue, 27 Mar 2007 19:36:42 +0000", res)
      makeAuthorization(accessKey, secretKey, stringToSign) mustEqual("AWS 0PN5J17HBGZHT7JJ3X82:xXjDGYUmKxnwqr5KXNPGldn5LbA=")
    }

  }

  "Make canonicalized headers" should {

    "create a conforming string from a simple header map" in {
      val str = makeCanonicalAmzHeaders(Map(
        "x-AMZ-foo" -> List("fooV"),
        "X-aMz-bar" -> List("barV")))

      str mustEqual ("x-amz-bar:barV\nx-amz-foo:fooV")
    }

    "only include x-amz headers" in {
      val str = makeCanonicalAmzHeaders(Map(
        "foo" -> List("fooV"),
        "X-aMz-bar" -> List("barV")))

      str mustEqual ("x-amz-bar:barV")
    }

    "properly normalise multiline headers" in {

      val str = makeCanonicalAmzHeaders(Map(
        "x-AMZ-foo" -> List("foo1\n  foo2 foo2  \nfoo3")))

      str mustEqual ("x-amz-foo:foo1 foo2 foo2 foo3")
    }

    "comma separate multiple values for keys" in {
      val str = makeCanonicalAmzHeaders(Map(
        "x-AMZ-foo" -> List("foo1", "foo2", "foo3")))

      str mustEqual ("x-amz-foo:foo1,foo2,foo3")
    }
  }

  "Make canonical resource" should {

    "make a confirming string for a simple resource" in {
      makeCanonicalResource("foo", "/bar/baz.jpg", "") mustEqual ("/foo/bar/baz.jpg")
    }

    "not include the bucket if not specified" in {
      makeCanonicalResource("", "/bar/baz.jpg", "") mustEqual ("/bar/baz.jpg")
    }

    "only include amazon query params in sorted order" in {
      makeCanonicalResource("foo", "/bar/baz.jpg", "?foo=123&website=456&acl=789") mustEqual ("/foo/bar/baz.jpg?acl=789&website=456")
    }

    "include query keys if key-only (i.e. blah?acl)" in pending

    "append overridden response parameters" in pending
  }

  "Make string to sign" should {

    "build confirming string" in {
      makeStringToSign("GET", "Date", "Resource", "Headers", "ContentType", "MD5") mustEqual (
        "GET\nMD5\nContentType\nDate\nHeaders\nResource")
    }

    "still include values as blank if not supplied" in {
      makeStringToSign("GET", "Date", "Resource", "Headers") mustEqual (
        "GET\n\n\nDate\nHeaders\nResource")
    }
  }

  "Make Authorisation" should {

    "Base64 an HMAC-SHA1 encoding of the provided string-to-sign" in {
      makeAuthorization("SOME_ACCESS_KEY", "SOME_SECRET_KEY", "SOME_STRING_TO_SIGN") mustEqual (
        "AWS SOME_ACCESS_KEY:JCZNqvAboyfLyCdRwxjNzpLQxeQ=")
    }
  }
}