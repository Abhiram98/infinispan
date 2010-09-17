#!/bin/bash

DIRNAME=`dirname $0`

# Setup ISPN_HOME
if [ "x$ISPN_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    ISPN_HOME=`cd $DIRNAME/..; pwd`
fi
export ISPN_HOME

CP=${CP}:${ISPN_HOME}/infinispan-core.jar

for i in ${ISPN_HOME}/lib/*.jar ; do
   CP=${i}:${CP}
done

for i in ${ISPN_HOME}/modules/lucene-directory-demo/*.jar ; do
   CP=${i}:${CP}
done

for i in ${ISPN_HOME}/modules/lucene-directory-demo/lib/*.jar ; do
   CP=${i}:${CP}
done

JVM_PARAMS="${JVM_PARAMS} -Dbind.address=127.0.0.1 -Djava.net.preferIPv4Stack=true -Dlog4j.configuration=file:${ISPN_HOME}/etc/log4j.xml"

# Sample JPDA settings for remote socket debugging
#JVM_PARAMS="$JVM_PARAMS -Xrunjdwp:transport=dt_socket,address=8686,server=y,suspend=n"

java -cp ${CP} ${JVM_PARAMS} org.infinispan.lucenedemo.DemoDriver
