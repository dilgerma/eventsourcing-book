#!/usr/bin/env node
// Standalone realtime agent — subscribes to board events and writes tasks.json.
// The same logic runs embedded inside ralph-claude.js / ralph-ollama.js, so you
// only need this if you want to run the agent independently (e.g. separate terminal).
// Usage: node realtime-agent.js [kit_dir]

import { loadLocalConfig, fetchPlatformConfig, retryOn401, startRealtimeAgent } from './lib/ralph.js';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';

const kitDir = process.argv[2] ? resolve(process.argv[2]) : dirname(fileURLToPath(import.meta.url));

const local = loadLocalConfig(kitDir);
const cfg = await retryOn401('fetchPlatformConfig', () => fetchPlatformConfig(local));

console.log(`[agent] Starting — org=${cfg.organizationId}, board=${cfg.boardId}, base=${cfg.baseUrl}`);

await startRealtimeAgent(cfg, kitDir);
