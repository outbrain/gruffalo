#!/bin/sh

mvn dependency:build-classpath -Dmdep.outputFile=classpath
CLASSPATH=target/classes:`cat classpath`
log4jfile=$(pwd)/src/main/resources/stand-alone-log4j.xml
java -cp $CLASSPATH -Dlog4j.configuration=file://$log4jfile -Xmx2024m -Xms2024m com.outbrain.gruffalo.StandaloneGruffaloServer "$@"
