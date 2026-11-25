@echo off
setlocal enabledelayedexpansion
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.10002-hotspot"
set "CP=target/test-classes;target/classes"
for %%j in (target\lib\*.jar) do set "CP=!CP!;%%j"
"%JAVA_HOME%\bin\java.exe" -cp "%CP%" br.com.ulife.TestLogin