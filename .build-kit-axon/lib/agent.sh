#!/bin/bash
# Runs the AI agent with the given prompt in the project directory.
# Called by ralph.sh with cwd already set to the project root.
# Usage: ./agent.sh "<prompt>"

set -euo pipefail

PROMPT="${1:-}"
if [[ -z "$PROMPT" ]]; then
  echo "ERROR: No prompt provided"
  exit 1
fi

claude --dangerously-skip-permissions -p "$PROMPT"

# --- To use a local Ollama model instead, comment out the line above
#     and uncomment the block below. Run `ollama serve` first.
#
# MODEL="${OLLAMA_MODEL:-qwen3.5:9b}"
# node "$(dirname "$0")/ollama-agent.js" "$MODEL"
