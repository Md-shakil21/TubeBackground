@echo off
chcp 65001 >nul
title TubeBackground - GitHub Upload Helper
echo ======================================================
echo    TubeBackground GitHub Auto-Uploader
echo ======================================================
echo.
echo আপনার ব্রাউজারে তৈরি করা GitHub রিপোজিটরির লিংকটি নিচে পেস্ট করুন।
echo (উদাহরণ: https://github.com/your-username/TubeBackground.git)
echo.
set /p REPO_URL="GitHub Repository URL: "

if "%REPO_URL%"=="" (
    echo.
    echo [ত্রুটি] কোনো লিংক দেওয়া হয়নি! স্ক্রিপ্ট বন্ধ করা হচ্ছে...
    pause
    exit /b
)

echo.
echo [1/3] Remote URL কনফিগার করা হচ্ছে...
git remote remove origin 2>nul
git remote add origin %REPO_URL%

echo [2/3] Branch নিশ্চিত করা হচ্ছে...
git branch -M main

echo [3/3] GitHub-এ পুশ করা হচ্ছে...
echo (যদি কোনো ব্রাউজার উইন্ডো পপ-আপ করে, তাহলে শুধু 'Sign in with your browser' এ ক্লিক করুন)
echo.
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================================
    echo [অভিনন্দন!] কোড সফলভাবে GitHub-এ আপলোড হয়ে গেছে!
    echo এখন আপনার GitHub রিপোজিটরির 'Actions' ট্যাবে যান।
    echo ২-৩ মিনিটের মধ্যে সেখানে রেডি APK পেয়ে যাবেন!
    echo ======================================================
) else (
    echo.
    echo [সমস্যা] কোড পুশ করতে সমস্যা হয়েছে। দয়া করে লিংক ও লগইন চেক করুন।
)

echo.
pause
