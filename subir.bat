@echo off
cd /d "%~dp0"
git branch -M dev
git push -u origin dev
pause
