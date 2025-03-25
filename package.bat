@echo off
chcp 65001
setlocal enabledelayedexpansion

:: 设置路径
set "JAVA_HOME=C:\Program Files\Java\jdk-1.8"
set "LAUNCH4J_HOME=C:\Program Files (x86)\Launch4j"

:: 清理并创建目录
echo 清理并创建目录...
if exist "release" rd /s /q "release"
mkdir release
mkdir release\lib
mkdir release\config

:: 复制文件
echo 复制文件...
copy "target\proxy-visitor-1.0-SNAPSHOT.jar" "release\lib\proxy-visitor.jar"

:: 创建配置文件
echo 创建配置文件...
(
echo # 目标网址列表
echo https://example.com
echo https://example.org
) > "release\config\urls.txt"

(
echo # 代理服务器配置
echo # 格式：代理地址 端口 用户名 密码
echo proxy1.example.com 1080 user1 pass1
echo proxy2.example.com 1080 user2 pass2
) > "release\config\proxies.txt"

:: Launch4j打包
echo Launch4j打包...
"%LAUNCH4J_HOME%\launch4jc.exe" launch4j-config.xml

:: 检查Launch4j打包结果
if not exist "ProxyVisitor.exe" (
    echo Launch4j打包失败！
    pause
    exit /b 1
)

:: 移动exe到release目录
move /y "ProxyVisitor.exe" "release\"

:: 创建启动脚本（备用）
echo 创建备用启动脚本...
(
echo @echo off
echo chcp 65001 ^>nul
echo start javaw -Dfile.encoding^=UTF-8 -jar lib\proxy-visitor.jar
) > "release\启动程序.bat"

echo 打包完成！
echo 程序文件在release目录中
pause