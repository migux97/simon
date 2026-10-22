#!/bin/sh
cd "$(dirname "$0")"
# Usa el nombre y correo configurados en esta PC (git config --global user.name / user.email)
git commit --amend --reset-author --no-edit
git branch -M dev
git push -u origin dev
