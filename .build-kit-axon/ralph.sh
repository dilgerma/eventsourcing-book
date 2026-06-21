#!/bin/bash
# Ralph agent loop — two independent loops, each triggered by their own condition
#
# onTask:        tasks.json has entries  → load slice from board, update .build-kit-axon/.slices/
# onPlannedSlice: .build-kit-axon/.slices/ has a "Planned" slice → build it
#
# The loops are NOT causally linked — either can trigger on its own.
#
# Usage: ./ralph.sh [iterations] [project_dir]
#   iterations  — number of loop cycles to run; 0 or omitted means run forever
#   project_dir — path to the project root; defaults to the parent of .build-kit-axon

set -euo pipefail

KIT_DIR="$(cd "$(dirname "$0")" && pwd)"
ITERATIONS="${1:-0}"
PROJECT_DIR="${2:-"$KIT_DIR/.."}"
TASKS_FILE="$KIT_DIR/tasks.json"
PROMPT_FILE="$KIT_DIR/lib/prompt.md"
BACKEND_PROMPT_FILE="$KIT_DIR/lib/backend-prompt.md"
AGENT_SCRIPT="$KIT_DIR/lib/agent.sh"

HAS_CREDENTIALS=true
if [[ ! -f "$KIT_DIR/.eventmodelers/config.json" ]]; then
  echo "[ralph] Note: no .eventmodelers/config.json found — platform sync disabled." >&2
  echo "        To enable board sync, follow: https://app.eventmodelers.ai/documentation#build-axon" >&2
  echo "        Code generation from local slice definitions will still run." >&2
  HAS_CREDENTIALS=false
fi

echo "Ralph — kit: $KIT_DIR  project: $PROJECT_DIR"

# Returns 0 if tasks.json has at least one task
has_pending_tasks() {
  [[ -f "$TASKS_FILE" ]] || return 1
  local content
  content=$(cat "$TASKS_FILE")
  [[ "$content" != "[]" && -n "$content" ]]
}

# Returns 0 if any JSON under .build-kit-axon/.slices/ contains a "Planned" status
has_planned_slices() {
  grep -rqi '"status"[[:space:]]*:[[:space:]]*"planned"' "$KIT_DIR/.slices/" --include='index.json' 2>/dev/null
}

# Returns the title of the first "Planned" slice, or empty string
get_planned_slice_title() {
  for index_file in "$KIT_DIR/.slices/"*/index.json; do
    [[ -f "$index_file" ]] || continue
    local title
    title=$(node -e "
      try {
        const d = JSON.parse(require('fs').readFileSync(process.argv[1], 'utf-8'));
        const s = (d.slices||[]).find(s => (s.status||'').toLowerCase() === 'planned');
        if (s) process.stdout.write(s.slice || s.id || '');
      } catch(e) {}
    " "$index_file" 2>/dev/null)
    if [[ -n "$title" ]]; then
      echo "$title"
      return
    fi
  done
}

# Runs agent.sh with the given prompt; retries on non-zero exit
run_agent() {
  local label="$1"
  local prompt="$2"
  while true; do
    echo "[$(date -u +%H:%M:%S)] $label"
    (cd "$PROJECT_DIR" && bash "$AGENT_SCRIPT" "$prompt") 2>&1 && return 0
    echo "[$(date -u +%H:%M:%S)] Agent error — retrying in 60s..."
    sleep 60
  done
}

cycle=0
while [[ "$ITERATIONS" -eq 0 || "$cycle" -lt "$ITERATIONS" ]]; do
  ran_something=false

  if [[ "$HAS_CREDENTIALS" == true ]] && has_pending_tasks; then
    run_agent "onTask: loading slice from board..." "$(cat "$PROMPT_FILE")"
    ran_something=true
  fi

  if has_planned_slices; then
    slice_title=$(get_planned_slice_title)
    run_agent "onPlannedSlice: building \"$slice_title\"..." "$(cat "$BACKEND_PROMPT_FILE")"
    echo "[$(date -u +%H:%M:%S)] Slice \"$slice_title\" build complete — waiting for next slice"
    ran_something=true
  fi

  if [[ "$ran_something" == false ]]; then
    sleep 3
  fi

  (( cycle++ )) || true
done
