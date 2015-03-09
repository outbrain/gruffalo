#!/bin/sh

mvn dependency:build-classpath -Dmdep.outputFile=classpath
export CLASSPATH=target/classes:`cat classpath`
export log4jfile=$(pwd)/src/main/resources/stand-alone-log4j.xml
echo $log4jfile
java -cp $CLASSPATH -Dlog4j.configuration=file://$log4jfile -Xmx2024m -Xms2024m com.outbrain.gruffalo.StandaloneGruffaloServer
