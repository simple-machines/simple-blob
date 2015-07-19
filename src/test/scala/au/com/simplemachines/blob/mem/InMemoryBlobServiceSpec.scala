package au.com.simplemachines.blob.mem

import java.io.ByteArrayInputStream
import java.nio.channels.Channels

import com.google.common.io.ByteStreams

class InMemoryBlobServiceSpec extends org.specs2.mutable.Specification {

  val someContent = "Content"

  "InMemoryBlobService" should {
    "Do CRUD ops as expected" in {
      val blobService = new InMemoryBlobService(30)

      val key = blobService.allocate("some-file.txt", "text/plain")

      blobService.update(key, Channels.newChannel(new ByteArrayInputStream(someContent.getBytes)), someContent.length())

      val info = blobService.getInfo(key)
      info.name mustEqual("some-file.txt")
      info.size mustEqual(someContent.length)
      info.mimeType mustEqual("text/plain")

      val storedContent = new String(ByteStreams.toByteArray(Channels.newInputStream(blobService.read(key))))

      storedContent mustEqual(someContent)
    }
  }

}