import co.freeside.betamax.MatchRule
import java.io.{BufferedReader, Reader}
import co.freeside.betamax.message.Request
import org.slf4j.LoggerFactory
import scala.xml.{XML, Utility}

class MyMatchRule extends MatchRule {
  val log = LoggerFactory.getLogger(this.getClass)

   def getBodyAsString(stream: Reader): String = {
     val br = new BufferedReader(stream)
     Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
   }

   def contentTypeXml(r: Request): Boolean = {
     val header = r.getHeader("Content-Type")
     header != null && header.toLowerCase.matches(".*xml.*")
   }

  def matchLogic(a:Request, b:Request): Boolean ={
    //we want to match on the host
    if (
      a.getUri == b.getUri &&
        a.getMethod == b.getMethod
    ) {
      log.info("METHOD AND URI MATCHES")

      log.info(s"DOES A HAVE A BODY: ${a.hasBody}")
      log.info(s"DOES B HAVE A BODY: ${b.hasBody}")
      //Methods match and URI's match
      val aBody = getBodyAsString(a.getBodyAsText.getInput)
      val bBody = getBodyAsString(b.getBodyAsText.getInput)

      //Determine if it's XML so we should normalize it
      if (contentTypeXml(a) && contentTypeXml(b)) {
        log.info("BOTH SIDES ARE XML")
        // we're dealing with XML, so it's normalizing time
        //Probably could've just compared it as XML elems, but this works
        try {
          Utility.trim(XML.loadString(aBody)) == Utility.trim(XML.loadString(bBody))
        } catch {
          case e: Exception =>
            //This could be thrown if I'm loading in a JSON to try to parse as XML.
            // Yeah we don't want to deal with that.
            false
        }
      } else {
        log.info("BOTH SIDES ARENT XML")
        log.info(s"A: ${aBody}")
        log.info(s"B: ${bBody}")
        //It's not xml, so we need to do something different
        //For now just compare the two strings
        aBody == bBody
      }
    } else {
      log.info("METHOD OR URI DIDNT MATCH")
      false
    }

  }

   def isMatch(a: Request, b: Request): Boolean = {
     val result = matchLogic(a,b)

     log.info(s"Result of matching: ${result}")

     result
   }

 }
