#!/bin/bash

# Picocli Demo 运行脚本
# Java工具学习 - Day 1

set -e

echo "================================================"
echo "  Picocli Demo 运行脚本"
echo "  Java工具学习 - Day 1"
echo "================================================"
echo ""

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "[错误] 未找到Maven，请先安装Maven"
    echo "下载地址: https://maven.apache.org/download.cgi"
    exit 1
fi

# 编译项目
echo "[1/4] 编译项目..."
mvn clean compile -q

echo "[2/4] 编译成功！"
echo ""

# 演示1: 基础问候
echo "================================================"
echo "  演示1: 基础问候"
echo "================================================"
mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" \
     -Dexec.args="greet --name 张三 --count 3" -q
echo ""

# 演示2: 子命令 - 用户创建
echo "================================================"
echo "  演示2: 子命令 - 用户创建"
echo "================================================"
mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" \
     -Dexec.args="user create --name 李四 --email li@example.com --role admin" -q
echo ""

# 演示3: 文件处理
echo "================================================"
echo "  演示3: 文件处理"
echo "================================================"
mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" \
     -Dexec.args="process --files file1.txt,file2.txt,file3.txt --parallel --verbose" -q
echo ""

# 演示4: 代码生成
echo "================================================"
echo "  演示4: 代码生成"
echo "================================================"
mvn exec:java -Dexec.mainClass="com.example.picocli.demo.MainCommandDemo" \
     -Dexec.args="generate entity --name User --table users --fields id:Long,name:String,email:String" -q
echo ""

echo "================================================"
echo "  演示完成！"
echo "================================================"
