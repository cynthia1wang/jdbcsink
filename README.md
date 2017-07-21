# jdbcsink
This test program uses structured streaming of spark, receive message that is json type from kafka,  then get counts of DNS for every people In one minute. (I think one source Ip addr is one people). Finally, the result will insert to mysql database.

# Building on Unix with sbt
  scala version is 2.11.8, if you not, you will change the build.sbt, modify
  
  scalaVersion:= "your version"
 
## Prerequisites:

spark-2.1.1-bin-hadoop2.6

KAFAK-2.1.1-1.2.1.1.po.18

mysql version：14.14

linux：centos 6.5（Final）
  
## Build instructions:

  git clone https://github.com/cynthia1wang/jdbcsink.git
  
  $cd jdbcsink/
  
  $sbt clean package
  
## Run the test programs:  
  $spark-submit --deploy-mode cluster --master yarn --supervise --driver-class-path mysql-connector-java-5.1.42-bin.jar --class DNSstatJob --jars spark-sql-kafka-0-10_2.11-2.1.1.jar,spark-streaming-kafka-0-10-assembly_2.11-2.1.1.jar,mysql-connector-java-5.1.42-bin.jar target/scala-2.11/dnsjob-project_2.11-1.0.jar
