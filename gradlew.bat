@rem Windows gradle wrapper stub.
@rem Mirrors the POSIX script. The actual jar must be downloaded from
@rem https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar
@echo off
setlocal
set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
  echo Gradle wrapper jar is missing at %WRAPPER_JAR%.
  echo Run "gradle wrapper" once with a local Gradle install, or download the jar from
  echo https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar
  exit /b 1
)
"%JAVA_HOME%\bin\java.exe" -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
