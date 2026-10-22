@echo off
cd /d "%~dp0"
rem Usa el nombre y correo configurados en esta PC (git config --global user.name / user.email)
git commit --amend --reset-author --no-edit
git branch -M dev
git push -u origin dev
pause
