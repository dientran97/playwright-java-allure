@echo off
rem
rem Thin wrapper that accepts the project's short command line and forwards it to Maven.
rem
rem   run.bat clean verify -PTEST -user=User1 -suiteXmlFolder=Login ^
rem           -suiteXmlFile=testsuite.xml --headless -browser=chrome
rem
rem Maven itself only understands -Dkey=value, so -user=..., -browser=... and
rem --headless are rewritten here. Everything else is passed through untouched.
rem
setlocal enabledelayedexpansion
set "MAVEN_ARGS="
set "HAS_GOAL=0"

:parse
if "%~1"=="" goto run
set "ARG=%~1"

if /I "!ARG!"=="--headless" (
    set "MAVEN_ARGS=!MAVEN_ARGS! -Dheadless=true"
    shift & goto parse
)
if /I "!ARG!"=="--headed" (
    set "MAVEN_ARGS=!MAVEN_ARGS! -Dheadless=false"
    shift & goto parse
)
if "!ARG:~0,2!"=="-D" (
    set "MAVEN_ARGS=!MAVEN_ARGS! !ARG!"
    shift & goto parse
)
if "!ARG:~0,2!"=="-P" (
    set "MAVEN_ARGS=!MAVEN_ARGS! !ARG!"
    shift & goto parse
)
if "!ARG:~0,1!"=="-" (
    rem -key=value  ->  -Dkey=value ; any other flag is passed through
    echo !ARG! | findstr /R "=" >nul
    if errorlevel 1 (
        set "MAVEN_ARGS=!MAVEN_ARGS! !ARG!"
    ) else (
        set "TRIMMED=!ARG:~1!"
        if "!TRIMMED:~0,1!"=="-" set "TRIMMED=!TRIMMED:~1!"
        set "MAVEN_ARGS=!MAVEN_ARGS! -D!TRIMMED!"
    )
    shift & goto parse
)

set "HAS_GOAL=1"
set "MAVEN_ARGS=!MAVEN_ARGS! !ARG!"
shift & goto parse

:run
if "!HAS_GOAL!"=="0" set "MAVEN_ARGS=clean verify !MAVEN_ARGS!"
echo mvn !MAVEN_ARGS!
call mvn !MAVEN_ARGS!
endlocal
