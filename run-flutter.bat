@echo off
setlocal

REM Always run from this Flutter project's folder, regardless of where this file is launched.
cd /d "%~dp0"

set "FLUTTER_BIN=flutter"
if exist "C:\src\flutter\bin\flutter.bat" set "FLUTTER_BIN=C:\src\flutter\bin\flutter.bat"

call "%FLUTTER_BIN%" pub get
if errorlevel 1 goto :failed

call "%FLUTTER_BIN%" run %*
goto :eof

:failed
echo.
echo Flutter dependencies could not be prepared. Check that Flutter is installed and try again.
exit /b 1
