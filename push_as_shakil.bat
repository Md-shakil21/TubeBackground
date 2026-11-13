@echo off
chcp 65001 >nul
title TubeBackground - GitHub Upload for Md-shakil21
color 0A
echo ================================================================
echo       TubeBackground - GitHub Code Upload (Md-shakil21)
echo ================================================================
echo.
echo রিমোট রিপোজিটরি কনফিগার করা হচ্ছে:
echo https://github.com/Md-shakil21/TubeBackground.git
echo.
git remote set-url origin https://github.com/Md-shakil21/TubeBackground.git
git branch -M main

echo GitHub-এ আপলোড শুরু হচ্ছে...
echo.
echo ----------------------------------------------------------------
echo [মনোযোগ দিন] একটি ব্রাউজার বা সাইন-ইন পপ-আপ আসলে:
echo শুধু 'Sign in with your browser' এ ক্লিক করে 'Authorize' বাটনে চাপ দিন।
echo ----------------------------------------------------------------
echo.

git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ================================================================
    echo [অভিনন্দন!] সফলভাবে কোড GitHub-এ আপলোড হয়ে গেছে!
    echo এখন স্বয়ংক্রিয়ভাবে ক্লাউডে APK তৈরি শুরু হচ্ছে...
    echo বিল্ড দেখতে ব্রাউজারে নিচের লিংকে যান:
    echo https://github.com/Md-shakil21/TubeBackground/actions
    echo ================================================================
) else (
    echo.
    echo [সতর্কতা] আপলোড সম্পন্ন হয়নি। দয়া করে ব্রাউজার সাইন-ইন সম্পন্ন করুন।
)

echo.
echo এই উইন্ডোটি বন্ধ করতে যেকোনো কি চাপুন...
pause >nul
