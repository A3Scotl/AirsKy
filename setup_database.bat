@echo off
echo =====================================================
echo DATABASE SETUP FOR AUTOMATED FLIGHT SYSTEM
echo =====================================================

echo.
echo Please enter your MySQL credentials:
set /p username=Username (default: root): 
if "%username%"=="" set username=root

echo.
echo Step 1: Setting up standard travel classes...
mysql -u %username% -p -e "SOURCE quick_setup.sql;" airsky_db

if %errorlevel% neq 0 (
    echo ERROR: Failed to setup travel classes
    pause
    exit /b 1
)

echo.
echo Step 2: Creating automatic seat generation triggers...
mysql -u %username% -p airsky_db < automatic_seat_generation_trigger.sql

if %errorlevel% neq 0 (
    echo ERROR: Failed to create triggers
    pause
    exit /b 1
)

echo.
echo Step 3: Verifying setup...
mysql -u %username% -p -e "SELECT id, class_name, price_multiplier FROM travel_classes ORDER BY price_multiplier;" airsky_db

echo.
echo Step 4: Checking triggers...
mysql -u %username% -p -e "SHOW TRIGGERS LIKE 'flights';" airsky_db

echo.
echo =====================================================
echo SETUP COMPLETE! You can now:
echo 1. Run: mvn spring-boot:run
echo 2. Test flight creation on Postman
echo =====================================================
pause