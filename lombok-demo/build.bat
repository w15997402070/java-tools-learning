@echo off
cd /d D:\ai\workbuddy\java-tools-learning\lombok-demo
D:\apache-maven-3.6.3\bin\mvn.cmd clean compile
if %ERRORLEVEL% EQU 0 (
    echo.
    echo === BUILD SUCCESS ===
) else (
    echo.
    echo === BUILD FAILED ===
)
