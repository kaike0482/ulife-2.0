#!/bin/sh
# Maven Wrapper Startup Script

# Resolve the directory of this script
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
MAVEN_PROJECTBASEDIR=`dirname "$PRG"`
export MAVEN_PROJECTBASEDIR

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Maven Wrapper JAR not found: $WRAPPER_JAR"
    exit 1
fi

exec java -jar "$WRAPPER_JAR" "$@"