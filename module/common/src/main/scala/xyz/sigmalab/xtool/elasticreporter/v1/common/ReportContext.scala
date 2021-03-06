package xyz.sigmalab.xtool.elasticreporter.v1.common

import xyz.sigmalab.xtool.elasticreporter.v1.common

trait ReportContext {

    type Formatter <: common.Formatter

    val formatter: Formatter

    def keyFor(name: String): String

    def time : data.Millis

    def zone: String

    def localdatetime: java.time.LocalDateTime

    def datetimeFormatter: java.time.format.DateTimeFormatter

    /**
      * Prepared values for this context
      * Every context is able to have some predefined values to let report generators use it!
      * Such as 'time', 'host', ...
      * @return
      */
    def vals: Seq[formatter.Val]

    /**
      * Prepared variables for this context
      * @return
      */
    def vars: Map[String, String]

    def id: String

    def localDateTimeAsString: String = localdatetime.format(datetimeFormatter)

    def meta : ReportContext.Meta = ReportContext.Meta(id, time, zone, localDateTimeAsString)

}

object ReportContext {

    case class Meta(
        id: String,
        time: data.Millis,
        zone: String,
        localDateTime: String
    )
}
