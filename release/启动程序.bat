@echo off
chcp 65001 >nul
start javaw -Dfile.encoding=UTF-8 -jar lib\proxy-visitor.jar
