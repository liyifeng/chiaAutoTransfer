@echo off
title=Chia auto transfer
set curDir=%cd%
set JAVA_HOME=%curDir%\jre1.8.0_202
set PATH=%JAVA_HOME%\bin
set CLASSPATH=.;%JAVA_HOME%\lib;%JAVA_HOME%\lib\tools.jar

:head
set option=""
echo --------------------------------------------------------------------------------------------
echo 1.��������
echo 2.�����ұ����ڴ��ڣ�������в�����ʱ����ڹ۲�������־��
echo --------------------------------------------------------------------------------------------
set /p option=��������Ҫִ�е����:
echo loading...

if "%option%"=="0" goto exit
if "%option%"=="" goto start
if "%option%"=="1" goto start
if "%option%"=="2" goto startAndKeepTerminal

:start
start javaw -jar chiaAutoTransfer.jar
exit

:startAndKeepTerminal
java -jar chiaAutoTransfer.jar
goto head
