@echo off
setlocal enabledelayedexpansion

:: Extraer IP desde GlobalConfig.java usando PowerShell
for /f "usebackq tokens=*" %%a in (`powershell -Command "(Get-Content FIXFINDER\src\main\java\com\fixfinder\config\GlobalConfig.java | Select-String 'CLOUD_IP = \"(.*)\";').Matches.Groups[1].Value"`) do set CLOUD_IP=%%a

if "%CLOUD_IP%"=="" (
    echo [ERROR] No se pudo encontrar la CLOUD_IP en GlobalConfig.java
    pause
    exit /b
)

echo [FIXFINDER] Iniciando limpieza en la nube (IP: %CLOUD_IP%)...
echo [INFO] Conectando a AWS EC2 para ejecutar DbClean...

ssh -i ffk.pem ubuntu@%CLOUD_IP% "java -cp fixfinder-server.jar com.fixfinder.DbClean"

pause
