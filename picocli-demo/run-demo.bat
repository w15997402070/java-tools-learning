@echo off
chcp 65001 > nul
echo ================================================
echo   Picocli Demo 运行脚本
echo   Java工具学习 - Day 1
echo ================================================
echo.

cd /d %~dp0

REM 检查Maven是否安装
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Maven，请先安装Maven
    echo 下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM 编译项目
echo.
echo [1/4] 编译项目...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo [错误] 编译失败！
    pause
    exit /b 1
)

echo [2/4] 编译成功！
echo.

REM 显示主命令帮助
echo ================================================
echo   主命令帮助
echo ================================================
call mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" -Dexec.args="--help" -q
echo.

REM 演示1: 基础问候
echo ================================================
echo   演示1: 基础问候
echo ================================================
call mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" -Dexec.args="greet --name 张三 --count 3" -q
echo.

REM 演示2: 子命令 - 用户创建
echo ================================================
echo   演示2: 子命令 - 用户创建
echo ================================================
call mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" -Dexec.args="user create --name 李四 --email li@example.com --role admin" -q
echo.

REM 演示3: 文件处理
echo ================================================
echo   演示3: 文件处理
echo ================================================
call mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" -Dexec.args="process --files file1.txt,file2.txt,file3.txt --parallel --verbose" -q
echo.

REM 演示4: 代码生成
echo ================================================
echo   演示4: 代码生成
echo ================================================
call mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" -Dexec.args="generate entity --name User --table users --fields id:Long,name:String,email:String" -q
echo.

echo ================================================
echo   演示完成！
echo ================================================
echo.
pause
