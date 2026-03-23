@echo off
cd /d "%~dp0"

if not exist .env.local (
  copy .env.example .env.local
  echo Created .env.local from .env.example — edit it to add your API keys.
)

if not exist node_modules (
  echo Installing dependencies...
  call npm install
)

echo Starting dev server...
npm run dev
