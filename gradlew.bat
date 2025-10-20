@echo off
REM Minimal gradle wrapper bootstrap for Windows (downloads Gradle if missing)
setlocal
set "GRADLE_VERSION=8.5"
set "WRAPPER_DIR=%~dp0\.gradle-wrapper"
set "INSTALL_DIR=%WRAPPER_DIR%\gradle-%GRADLE_VERSION%"
if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
if not exist "%INSTALL_DIR%\bin\gradle.bat" (
  echo Downloading Gradle %GRADLE_VERSION% to "%WRAPPER_DIR%"...
  powershell -NoProfile -Command "Try { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile (Join-Path -Path '%WRAPPER_DIR%' -ChildPath 'gradle.zip') -UseBasicParsing } Catch { Exit 2 }"
  if errorlevel 1 (
    echo Failed to download Gradle. Please check your internet connection.
    exit /b 2
  )
  powershell -NoProfile -Command "Try { Expand-Archive -LiteralPath (Join-Path -Path '%WRAPPER_DIR%' -ChildPath 'gradle.zip') -DestinationPath '%WRAPPER_DIR%' -Force } Catch { Exit 3 }"
  if errorlevel 1 (
    echo Failed to extract Gradle archive.
    exit /b 3
  )
)
"%INSTALL_DIR%\bin\gradle.bat" %*
endlocal
