import co.freeside.betamax.{TapeMode, Recorder, ProxyConfiguration}
import java.io.File
import java.net.URL
import org.apache.commons.codec.digest.DigestUtils
import org.scalatest.{Matchers, FunSpec}
import uk.co.bigbeeconsultants.http.header.MediaType
import uk.co.bigbeeconsultants.http.request.RequestBody
import uk.co.bigbeeconsultants.http.{HttpClient, Config}

class TapeTest extends FunSpec
with Matchers
 {

  def md5SumGoogleTape():String = {
    val googleTapeStream = getClass.getResourceAsStream("/betamax/tapes/googletape.yaml")
    val value = DigestUtils.md2Hex(googleTapeStream)
    googleTapeStream.close
    value
  }


  describe("Proxy Testing with a tape") {
    it("Doesn't change the tape when replaying an HTTPS connection") {

      val startGoogleTapeSum = md5SumGoogleTape()

      val tapeRoot = new File("src/test/resources/betamax/tapes")
      val configBuilder = ProxyConfiguration.builder()
      configBuilder.sslEnabled(true)
      configBuilder.tapeRoot(tapeRoot)
      configBuilder.defaultMode(TapeMode.READ_WRITE)
      configBuilder.defaultMatchRule(new MyMatchRule)
      val proxyConfig = configBuilder.build
      val recorder = new Recorder(proxyConfig)

      recorder.start("googletape")

      val conf = Config(
        sslSocketFactory = Some(SSLValidation.socketFactory),
        hostnameVerifier = Some(SSLValidation.hostnameVerifier),
        proxy = None
      )
      val client = new HttpClient(conf)
      val url = new URL("https://www.google.com/test")
      val postPayload = "BUTTS"

      val response = client.post(url, Some(RequestBody(postPayload,MediaType.TEXT_PLAIN)))

      response.status.code should equal(200)
      response.body.asString should equal("Hey look some text: BUTTS")

      recorder.stop()

      //Validate that the tape has not been modified, since we're playing back
      val endGoogleTapeSum = md5SumGoogleTape()

      endGoogleTapeSum should equal(startGoogleTapeSum)
    }

  }

}
