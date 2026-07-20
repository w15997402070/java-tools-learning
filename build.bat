@echo off
D:\apache-maven-3.6.3\bin\mvn.cmd -f D:\ai\workbuddy\java-tools-learning\lombok-demo\pom.xml clean compile
echo.
echo Build finished with exit code: %ERRORLEVEL%
