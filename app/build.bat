@echo off
REM ---------------------------------------------------------------
REM Compiles the marketplace application and packages marketplace.jar
REM
REM Requires a JDK (javac). A JRE alone is not enough to build, though
REM a JRE is all that is needed to RUN the finished jar.
REM
REM The classes are targeted at Java 8 so the jar runs on the Java
REM runtime that ships with most machines. On a modern JDK that is
REM done with --release 8; on a JDK 8 that flag does not exist, so the
REM script falls back to a plain compile.
REM ---------------------------------------------------------------
setlocal

set DRIVER=lib\mysql-connector-j-8.0.33.jar
set LAF=lib\flatlaf-3.4.1.jar
set CP=%DRIVER%;%LAF%

where javac >nul 2>nul
if errorlevel 1 (
    if defined JAVA_HOME (
        set "JAVAC=%JAVA_HOME%\bin\javac.exe"
        set "JARTOOL=%JAVA_HOME%\bin\jar.exe"
    ) else (
        echo javac was not found on PATH and JAVA_HOME is not set.
        echo Install a JDK, or set JAVA_HOME to an existing JDK folder.
        exit /b 1
    )
) else (
    set "JAVAC=javac"
    set "JARTOOL=jar"
)

if not exist "%DRIVER%" (
    echo Missing %DRIVER%
    exit /b 1
)
if not exist "%LAF%" (
    echo Missing %LAF%
    exit /b 1
)

if exist out rmdir /s /q out
mkdir out

echo Compiling for Java 8...
"%JAVAC%" --release 8 -nowarn -encoding UTF-8 -d out -cp "%CP%" src\marketplace\*.java 2>nul
if errorlevel 1 (
    echo   --release not supported, compiling with the default target...
    "%JAVAC%" -nowarn -encoding UTF-8 -d out -cp "%CP%" src\marketplace\*.java
    if errorlevel 1 (
        echo Compilation failed.
        exit /b 1
    )
)

echo Packaging marketplace.jar...
"%JARTOOL%" cfm marketplace.jar manifest.txt -C out .
if errorlevel 1 (
    echo Packaging failed.
    exit /b 1
)

echo.
echo Build complete. Start the application with run.bat
endlocal
