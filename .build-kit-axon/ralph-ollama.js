#!/usr/bin/env node
// Ralph loop + realtime agent using a local Ollama model as the executor.
// Run `ollama serve` first.
// Usage: node ralph-ollama.js [project_dir]
//        OLLAMA_MODEL=qwen3:8b node ralph-ollama.js
//        OLLAMA_URL=http://host:11434 node ralph-ollama.js

import { startRalph } from './lib/ralph.js';
import { spawn } from 'child_process';
import { dirname, join, resolve } from 'path';
import { fileURLToPath } from 'url';

const kitDir = dirname(fileURLToPath(import.meta.url));
const projectDir = process.argv[2] ? resolve(process.argv[2]) : resolve(kitDir, '..');
const model = process.env.OLLAMA_MODEL || 'qwen3:8b';

console.log(`[ralph-ollama] model=${model}`);

function runOllama() {
  return new Promise((resolve, reject) => {
    const proc = spawn('node', [join(kitDir, 'lib', 'ollama-agent.js'), model], {
      cwd: projectDir,
      stdio: 'inherit',
      env: process.env,
    });
    proc.on('close', (code) => (code === 0 ? resolve() : reject(new Error(`ollama-agent exited ${code}`))));
    proc.on('error', reject);
  });
}

startRalph({
  kitDir,
  projectDir,
  onTask: runOllama,
  // onPlannedSlice omitted — ollama-agent manages its own task queue
}).catch((err) => {
  console.error('[ralph] Fatal:', err);
  process.exit(1);
});
