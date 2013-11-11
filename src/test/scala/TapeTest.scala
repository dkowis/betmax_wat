import co.freeside.betamax.{TapeMode, Recorder, ProxyConfiguration}
import java.io.File
import java.net.URL
import org.apache.commons.codec.digest.DigestUtils
import org.scalatest.{Matchers, FunSpec}
import uk.co.bigbeeconsultants.http.{HttpClient, Config}

class TapeTest extends FunSpec
with Matchers
 {

  describe("Proxy Testing with a tape") {
    it("Doesn't change the tape when replaying an HTTPS connection") {

      val tapeRoot = new File("src/test/resources/betamax/tapes")
      val configBuilder = ProxyConfiguration.builder()
      configBuilder.sslEnabled(true)
      configBuilder.tapeRoot(tapeRoot)
      configBuilder.defaultMode(TapeMode.READ_WRITE)

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
      val response = client.get(url)

      response.status.code should equal(200)
      response.body.asString should equal("Hey look some text")

      recorder.stop()

      //Validate that the tape has not been modified, since we're playing back
      val googleTapemd5sum = "b0770ac83863c69bd300a1d320d83acb"

      //Calculate the md5sum of the googletape again, and it should be the same
      val googleTapeStream = getClass.getResourceAsStream("/betamax/tapes/googletape.yaml")

      val calculated = DigestUtils.md5Hex(googleTapeStream)

      calculated should equal(googleTapemd5sum)
    }

  }

}
