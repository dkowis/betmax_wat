import co.freeside.betamax.{TapeMode, Recorder, ProxyConfiguration}
import java.io.File
import java.net.URL
import org.apache.commons.codec.digest.DigestUtils
import org.scalatest.{Matchers, FunSpec}
import uk.co.bigbeeconsultants.http.header.MediaType
import uk.co.bigbeeconsultants.http.request.RequestBody
import uk.co.bigbeeconsultants.http.{HttpClient, Config}

class TapeTest extends FunSpec
with Matchers {

  val tapeName = "httpbinTape"

  def md5SumTestTape(name: String): Option[String] = {
    val googleTapeStream = getClass.getResourceAsStream(s"/betamax/tapes/${name}.yaml")
    if (googleTapeStream != null) {
      val value = DigestUtils.md2Hex(googleTapeStream)
      googleTapeStream.close
      Some(value)
    } else {
      None
    }
  }


  describe("Proxy Testing with a tape") {
    it("Doesn't change the tape when replaying an HTTPS connection") {

      val startGoogleTapeSum = md5SumTestTape(tapeName)

      val tapeRoot = new File("src/test/resources/betamax/tapes")
      val configBuilder = ProxyConfiguration.builder()
      configBuilder.sslEnabled(true)
      configBuilder.tapeRoot(tapeRoot)
      configBuilder.defaultMode(TapeMode.READ_WRITE)
      configBuilder.defaultMatchRule(new MyMatchRule)
      val proxyConfig = configBuilder.build
      val recorder = new Recorder(proxyConfig)

      recorder.start(tapeName)

      val conf = Config(
        sslSocketFactory = Some(SSLValidation.socketFactory),
        hostnameVerifier = Some(SSLValidation.hostnameVerifier),
        proxy = None
      )
      val client = new HttpClient(conf)
      val url = new URL("https://httpbin.org/post")
      val postPayload = "BUTTS"

      val response = client.post(url, Some(RequestBody(postPayload, MediaType.TEXT_PLAIN)))

      //Stop the recorder immeidately, to avoid kabooms
      recorder.stop()

      //lets check the tape problem first, because it shouldn't have changed.
      if(startGoogleTapeSum.isDefined) {
        //Validate that the tape has not been modified, since we're playing back
        val endGoogleTapeSum = md5SumTestTape(tapeName).get

        endGoogleTapeSum should equal(startGoogleTapeSum.get)
      }

      //Now we can validate our response
      response.status.code should equal(200)
      response.body.asString should equal("Hey look some text: BUTTS")
    }

  }

}
