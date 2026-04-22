@echo off
set /p token=Enter token:
adb shell am start -a android.intent.action.VIEW -d "polytoria://client/%token%"
pause
