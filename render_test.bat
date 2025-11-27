@echo off
REM render_test.bat - Test script for running the web renderer on Windows
REM Usage: render_test.bat [python|rust|all]

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set TEST_DIR=%SCRIPT_DIR%org.web.labs.inside.jerry\src\jerry\test
set PYTHON_DIR=%SCRIPT_DIR%org.web.labs.inside.jerry\src\jerry\python
set RUST_DIR=%SCRIPT_DIR%org.web.labs.inside.jerry\src\jerry\rust
set OUTPUT_DIR=%SCRIPT_DIR%output

REM Default test files
set HTML_FILE=%TEST_DIR%\test.html
set CSS_FILE=%TEST_DIR%\test.css

REM Create output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Parse arguments
set COMMAND=all
set SHOW_HELP=0

:parse_args
if "%~1"=="" goto :run_command
if /i "%~1"=="python" (
    set COMMAND=python
    shift
    goto :parse_args
)
if /i "%~1"=="rust" (
    set COMMAND=rust
    shift
    goto :parse_args
)
if /i "%~1"=="all" (
    set COMMAND=all
    shift
    goto :parse_args
)
if /i "%~1"=="help" (
    goto :show_help
)
if /i "%~1"=="-h" (
    goto :show_help
)
if /i "%~1"=="--help" (
    goto :show_help
)
if /i "%~1"=="--html" (
    set HTML_FILE=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--css" (
    set CSS_FILE=%~2
    shift
    shift
    goto :parse_args
)
echo Unknown option: %~1
goto :show_help

:run_command
REM Verify test files exist
if not exist "%HTML_FILE%" (
    echo Error: HTML file not found: %HTML_FILE%
    exit /b 1
)
if not exist "%CSS_FILE%" (
    echo Error: CSS file not found: %CSS_FILE%
    exit /b 1
)

if /i "%COMMAND%"=="python" goto :run_python
if /i "%COMMAND%"=="rust" goto :run_rust
if /i "%COMMAND%"=="all" goto :run_all
goto :show_help

:run_python
echo.
echo ========================================
echo Running Python Renderer
echo ========================================
echo.

cd /d "%PYTHON_DIR%"

where python >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: Python is not installed
    exit /b 1
)

echo HTML: %HTML_FILE%
echo CSS: %CSS_FILE%
echo.

python main.py -H "%HTML_FILE%" -c "%CSS_FILE%" -o "%OUTPUT_DIR%\output_python.png" -v

if exist "%OUTPUT_DIR%\output_python.png" (
    echo.
    echo [OK] Python output saved to: %OUTPUT_DIR%\output_python.png
) else (
    echo.
    echo [!] Output file not created (Pillow may not be installed)
)
goto :eof

:run_rust
echo.
echo ========================================
echo Running Rust Renderer
echo ========================================
echo.

cd /d "%RUST_DIR%"

where cargo >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: Rust/Cargo is not installed
    exit /b 1
)

echo Building Rust project...
cargo build --release 2>nul
if %errorlevel% neq 0 (
    cargo build
)

echo.
echo HTML: %HTML_FILE%
echo CSS: %CSS_FILE%
echo.

cargo run --release -- -h "%HTML_FILE%" -c "%CSS_FILE%" -o "%OUTPUT_DIR%\output_rust.png" 2>nul
if %errorlevel% neq 0 (
    cargo run -- -h "%HTML_FILE%" -c "%CSS_FILE%" -o "%OUTPUT_DIR%\output_rust.png"
)

if exist "%OUTPUT_DIR%\output_rust.png" (
    echo.
    echo [OK] Rust output saved to: %OUTPUT_DIR%\output_rust.png
)
goto :eof

:run_all
call :run_python
call :run_rust

echo.
echo ========================================
echo Comparison
echo ========================================
echo Python output: %OUTPUT_DIR%\output_python.png
echo Rust output:   %OUTPUT_DIR%\output_rust.png
echo.
echo Compare the outputs to verify they match!
goto :done

:show_help
echo Jerry Web Renderer - Test Script
echo.
echo Usage: %~nx0 [command] [options]
echo.
echo Commands:
echo   python    Run Python renderer only
echo   rust      Run Rust renderer only
echo   all       Run both renderers (default)
echo   help      Show this help message
echo.
echo Options:
echo   --html ^<file^>    Specify HTML file (default: test.html)
echo   --css ^<file^>     Specify CSS file (default: test.css)
echo.
echo Examples:
echo   %~nx0 python
echo   %~nx0 rust --html custom.html --css custom.css
echo   %~nx0 all
goto :eof

:done
echo.
echo ========================================
echo Done!
echo ========================================
