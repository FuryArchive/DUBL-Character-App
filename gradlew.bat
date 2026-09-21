@echo off
setlocal
set VERSION=9.7.0
set SHA256=84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae

if "%GRADLE_USER_HOME%"=="" (
  set BASE=%USERPROFILE%\.gradle\dubl-bootstrap
) else (
  set BASE=%GRADLE_USER_HOME%\dubl-bootstrap
)

set HOME_DIR=%BASE%\gradle-%VERSION%
set ZIP=%BASE%\gradle-%VERSION%-bin.zip
set URL=https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip

if not exist "%HOME_DIR%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"

  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP%';" ^
    "$actual=(Get-FileHash -Algorithm SHA256 '%ZIP%').Hash.ToLowerInvariant();" ^
    "if ($actual -ne '%SHA256%') { throw ('Gradle SHA-256 mismatch: ' + $actual) };" ^
    "Expand-Archive -Path '%ZIP%' -DestinationPath '%BASE%' -Force"

  if errorlevel 1 exit /b 1
)

call "%HOME_DIR%\bin\gradle.bat" %*
