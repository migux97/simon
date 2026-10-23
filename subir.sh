#!/bin/sh
cd "$(dirname "$0")"
git branch -M dev
git push -u origin dev
