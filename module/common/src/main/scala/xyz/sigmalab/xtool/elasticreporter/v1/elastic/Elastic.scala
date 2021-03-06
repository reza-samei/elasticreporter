package xyz.sigmalab.xtool.elasticreporter.v1.elastic

import java.io._

import org.slf4j.LoggerFactory
import java.net.{HttpURLConnection, URL}

import xyz.sigmalab.xtool.elasticreporter.v1.common

import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

case class Elastic(name: String, host: String) {



    private val logger = LoggerFactory.getLogger(name)

    logger.debug(s"Init ..., Class: ${getClass.getName}")


    httpRequest(host) { http =>
        http.connect()
        val body = readStream(http.getInputStream)
        body
    } match {
        case Success(body) =>
            logger.info(s"Init, Host: ${host}, Elastic: ${body}")
        case Failure(cause) =>
            logger.warn(s"Init, Host: ${host}, Failure: ${cause.getMessage}", cause)
            throw Elastic.InitException("Init Failure", host, Option(cause))
    }

    def put(report: Reporter.Report): Try[Unit] = {

        val url = s"${host}/${report.index}/doc/${report.doc}"

        if (logger.isTraceEnabled()) logger.trace(s"PUT, URL: ${url}, Req(${report.body.getBytes.size}): ${report.body}")

        httpRequest(url) { http =>
            http setUseCaches false
            http setDoOutput true
            http setRequestMethod "PUT"
            http setRequestProperty("Content-Type", "application/json")

            val output = new DataOutputStream(http.getOutputStream);
            val bytes = report.body.getBytes()

            output.write(bytes)

            output.flush()
            output.close()

            http.connect()

            http.getResponseCode match {
                case 200 | 201 =>
                    if (logger.isDebugEnabled()) {
                        val body = readStream(http.getInputStream)
                        logger.debug(s"PUT, URL: ${url}, Req(${bytes.length}), Res(${body.getBytes.length})")
                        logger.trace(s"PUT, URL: ${url}, Res(${body.getBytes.length}): ${body}")
                    }
                case unexp =>
                    if (logger.isWarnEnabled()) {
                        val body = readStream(http.getErrorStream)
                        logger.warn(s"PUT, URL: ${url}, Req(${bytes.length}): ${report.body}, Res(${body.getBytes.length}): ${body}")
                    }
                    throw Elastic.PutException(s"Put Failure, Unexpected ResponseCode: ${unexp}", report)
            }
        }
    }

    protected def httpRequest[T](url: String)(fn: HttpURLConnection => T): Try[T] = {
        var http: HttpURLConnection = null
        try {

            val rsl = Try {
                http = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
                // https://stackoverflow.com/questions/16688524/httpurlconnection-does-not-free-resources
                // System.setProperty("http.keepAlive", "false")       // ???
                // System.setProperty("http.maxConnections", "5")      // ???
                fn(http)
            }

            Try { http.getInputStream.close };
            Try { http.getErrorStream.close };
            Try { http.disconnect() };

            rsl

        } catch {
            case NonFatal(cause) =>

                Try { http.getInputStream.close };
                Try { http.getErrorStream.close };
                Try { http.disconnect() };

                Failure(cause)
        }
    }

    protected def readStream(input: InputStream): String = {
        val in = new BufferedReader(new InputStreamReader(input))
        val buf = new StringBuilder

        var line: String = in.readLine()
        while(line != null) {
            buf.append(line)
            line = in.readLine()
        }

        buf.result()
    }

}

object Elastic {

    case class PutException(
        desc: String,
        report: Reporter.Report
    ) extends RuntimeException(desc) with common.data.BaseException

    case class InitException(
        desc: String, host: String, cause: Option[Throwable]
    ) extends RuntimeException(desc, cause.orNull) with common.data.BaseException
}
