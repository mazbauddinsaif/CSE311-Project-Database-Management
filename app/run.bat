@echo off
REM ---------------------------------------------------------------
REM Starts the marketplace application.
REM Make sure Apache and MySQL are running in the XAMPP control panel
REM and that db\schema.sql, seed.sql and views.sql have been imported.
REM ---------------------------------------------------------------
setlocal

if not exist marketplace.jar (
    echo marketplace.jar not found. Run build.bat first.
    exit /b 1
)

java -jar marketplace.jar
endlocal
