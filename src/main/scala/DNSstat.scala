import org.apache.spark.sql.SparkSession
import java.util.Properties
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions.{from_json,window}
import java.sql.{Connection,Statement,DriverManager}
import org.apache.spark.sql.ForeachWriter
import org.apache.spark.sql.Row

object DNSstatJob{

val schema: StructType = StructType(
        Seq(StructField("Vendor", StringType,true),
         StructField("Id", IntegerType,true),
         StructField("Time", LongType,true),
         StructField("Conn", StructType(Seq(
                                        StructField("Proto", IntegerType, true), 
                                        StructField("Sport", IntegerType, true), 
                                        StructField("Dport", IntegerType, true), 
                                        StructField("Sip", StringType, true), 
                                        StructField("Dip", StringType, true)
                                        )), true),
        StructField("Dns", StructType(Seq(
                                        StructField("Domain", StringType, true), 
                                        StructField("IpCount", IntegerType, true), 
                                        StructField("Ip", StringType, true) 
                                        )), true)))

class JDBCSink() extends ForeachWriter[Row]{
 val driver = "com.mysql.jdbc.Driver"
      var connection:Connection = _
      var statement:Statement = _

    def open(partitionId: Long,version: Long): Boolean = {
        Class.forName(driver)
        connection = DriverManager.getConnection("jdbc:mysql://10.88.1.102:3306/aptwebservice", "root", "mysqladmin")
        statement = connection.createStatement
        true
      }
      def process(value: Row): Unit = {
        statement.executeUpdate("replace into DNSStat(ip,domain,time,count) values(" 
                                    + "'" + value.getString(0) + "'" + ","//ip
                                    + "'" + value.getString(1) + "'" + ","//domain
                                    + "'" + value.getTimestamp(2) + "'" + "," //time
                                    + value.getLong(3) //count
                                    + ")") 
      }

      def close(errorOrNull: Throwable): Unit = {
        connection.close
      }
}

    def main(args: Array[String]) {
    val spark=SparkSession
          .builder
          .appName("DNSJob")
          .config("spark.some.config.option", "some-value")
          .getOrCreate()
    import spark.implicits._
    val connectionProperties = new Properties()
    connectionProperties.put("user", "root")
    connectionProperties.put("password", "mysqladmin")
    val bruteForceTab = spark.read
                .jdbc("jdbc:mysql://10.88.1.102:3306/aptwebservice", "DNSTab",connectionProperties)
    bruteForceTab.registerTempTable("DNSTab")
    val lines = spark
          .readStream
          .format("kafka")
          .option("kafka.bootstrap.servers", "10.94.1.110:9092")
          .option("subscribe","xdr")
          //.option("startingOffsets","earliest")
          .option("startingOffsets","latest")
          .load()
          .select(from_json($"value".cast(StringType),schema).as("jsonData"))
    lines.registerTempTable("xdr")
    val filterDNS = spark.sql("select CAST(from_unixtime(xdr.jsonData.Time DIV 1000000) as timestamp) as time,xdr.jsonData.Conn.Sip as sip, xdr.jsonData.Dns.Domain from xdr inner join DNSTab on xdr.jsonData.Dns.domain = DNSTab.domain")
    
    val windowedCounts = filterDNS
                        .withWatermark("time","5 minutes")
                        .groupBy(window($"time", "1 minutes", "1 minutes"),$"sip",$"domain")
                        .count()
                        .select($"sip",$"domain",$"window.start",$"count")

    val writer = new JDBCSink()
    val query = windowedCounts
       .writeStream
        .foreach(writer)
        .outputMode("update")
        .option("checkpointLocation","/checkpoint/")
        .start()
        query.awaitTermination() 
   } 
}
