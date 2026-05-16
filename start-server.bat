@echo off
REM ===========================================================
REM SavingsGoal — one-click backend launcher
REM Starts MariaDB (port 3347) and the PHP REST API (port 8080)
REM so the Android emulator can reach http://10.0.2.2:8080/.
REM ===========================================================

setlocal
set "PROJECT_DIR=%~dp0"
set "API_DIR=%PROJECT_DIR%savingsapi"
set "XAMPP=C:\xampp"

echo.
echo === SavingsGoal backend launcher ===
echo.

REM --- 1. MariaDB on 3347 ----------------------------------
netstat -ano | findstr ":3347" | findstr LISTENING >nul
if errorlevel 1 (
    echo [1/2] Starting MariaDB on port 3347 ...
    start "SavingsGoal-MariaDB" /min "%XAMPP%\mysql\bin\mysqld.exe" --defaults-file="%XAMPP%\mysql\bin\my.ini" --standalone
    REM Wait until the port is actually listening (max ~15s)
    for /L %%i in (1,1,15) do (
        timeout /t 1 /nobreak >nul
        netstat -ano | findstr ":3347" | findstr LISTENING >nul && goto :db_ready
    )
    echo     WARNING: MariaDB did not open port 3347 in time.
    goto :db_done
    :db_ready
    echo     MariaDB is up on 3347.
    :db_done
) else (
    echo [1/2] MariaDB already running on 3347.
)

REM --- 2. PHP REST API on 8080 -----------------------------
netstat -ano | findstr ":8080" | findstr LISTENING >nul
if errorlevel 1 (
    echo [2/2] Starting PHP REST API on port 8080 ...
    start "SavingsGoal-API" "%XAMPP%\php\php.exe" -S 0.0.0.0:8080 -t "%API_DIR%"
    for /L %%i in (1,1,10) do (
        timeout /t 1 /nobreak >nul
        netstat -ano | findstr ":8080" | findstr LISTENING >nul && goto :api_ready
    )
    echo     WARNING: PHP API did not open port 8080 in time.
    goto :api_done
    :api_ready
    echo     PHP API is up on 8080.
    :api_done
) else (
    echo [2/2] PHP API already running on 8080.
)

echo.
echo Done. Endpoints:
echo   POST  http://10.0.2.2:8080/register.php   (from emulator)
echo   POST  http://10.0.2.2:8080/login.php      (from emulator)
echo   GET   http://127.0.0.1:8080/login.php     (from this PC, should return 405)
echo.
echo Leave the two spawned windows open while you use the app.
echo.
pause
endlocal
