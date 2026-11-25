@echo off
REM Maven Wrapper Startup Script for Windows

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

if not exist "%WRAPPER_JAR%" (
    echo Maven Wrapper JAR not found: %WRAPPER_JAR%
    exit /b 1
)

java -jar "%WRAPPER_JAR%" %*