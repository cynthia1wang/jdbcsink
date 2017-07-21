name := "dnsJob Project"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq("org.apache.spark" %% "spark-core" % "2.1.1",
"org.apache.spark" %% "spark-sql-kafka-0-10" % "2.1.1",
"org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.1.1",
"org.apache.spark" %% "spark-sql" % "2.1.1",
"org.mongodb.spark" % "mongo-spark-connector_2.11" % "2.0.0-rc0",
"org.apache.spark" % "spark-core_2.11" % "2.1.1"
)
