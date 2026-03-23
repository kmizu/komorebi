#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

PIDFILE=".komorebi.pids"

if [ ! -f "$PIDFILE" ]; then
  echo "No running processes found."
  exit 0
fi

read -r SBT_PID NEXT_PID < "$PIDFILE"

for pid in $SBT_PID $NEXT_PID; do
  if kill -0 "$pid" 2>/dev/null; then
    echo "Stopping PID $pid..."
    kill "$pid" 2>/dev/null || true
  fi
done

rm -f "$PIDFILE"
echo "Stopped."
