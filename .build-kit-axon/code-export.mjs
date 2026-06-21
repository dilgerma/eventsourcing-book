#!/usr/bin/env node
import { createServer } from 'http';
import { readFileSync, writeFileSync, existsSync, readdirSync, statSync, unlinkSync, mkdirSync } from 'fs';
import { join, dirname, relative } from 'path';
import { execSync } from 'child_process';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const ROOT = __dirname;
const CONFIG_PATH = join(ROOT, 'config.json');
const SLICES_DIR = join(ROOT, '.slices');
const repoPath = process.env.WORKSPACE_PATH ?? join(__dirname, '..');

// ---------------------------------------
// Helpers
// ---------------------------------------
function sendJSON(res, data, status = 200) {
    res.writeHead(status, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, Authorization' });
    res.end(JSON.stringify(data, null, 2));
}

function parseIdWithRegex(filename) {
    const match = filename.match(/screen-(\d+)\.png$/);
    return match ? match[1] : null;
}

function slugify(text) {
    return text
        .toString()
        .toLowerCase()
        .trim()
        .replace(/\s+/g, '-')
        .replace(/[^\w\-]+/g, '')
        .replace(/\-\-+/g, '-')
        .replace(/^-+/, '')
        .replace(/-+$/, '');
}

function findFilesRecursive(dir, filterFn, results = []) {
    if (!existsSync(dir)) return results;
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        const fullPath = join(dir, entry.name);
        if (entry.isDirectory()) findFilesRecursive(fullPath, filterFn, results);
        else if (entry.isFile() && filterFn(entry.name)) results.push(fullPath);
    }
    return results;
}

function isGitRepo(path) {
    try {
        execSync('git rev-parse --git-dir', { cwd: path, stdio: 'ignore' });
        return true;
    } catch {
        return false;
    }
}

function gitLsTree(path, revision, dir) {
    try {
        const output = execSync(`git ls-tree -r --name-only ${revision} -- ${dir}`, { cwd: path, encoding: 'utf-8' });
        return output.split('\n').filter(Boolean);
    } catch {
        return [];
    }
}

function gitShow(path, ref) {
    try {
        return execSync(`git show ${ref}`, { cwd: path, encoding: 'utf-8' });
    } catch {
        return null;
    }
}

function gitInit(path) {
    try {
        execSync('git init', { cwd: path, stdio: 'ignore' });
        return true;
    } catch {
        return false;
    }
}

function gitAdd(path, files) {
    try {
        execSync(`git add ${files}`, { cwd: path, stdio: 'ignore' });
        return true;
    } catch {
        return false;
    }
}

function gitStatus(path) {
    try {
        const output = execSync('git status --porcelain', { cwd: path, encoding: 'utf-8' });
        const lines = output.split('\n').filter(Boolean);
        const staged = lines.filter(line => /^[MARC]/.test(line)).map(line => line.substring(3));
        return { staged };
    } catch {
        return { staged: [] };
    }
}

function gitCommit(path, message, files) {
    try {
        const output = execSync(`git commit -m "${message}" ${files}`, { cwd: path, encoding: 'utf-8' });
        const branch = execSync('git rev-parse --abbrev-ref HEAD', { cwd: path, encoding: 'utf-8' }).trim();
        const commit = execSync('git rev-parse HEAD', { cwd: path, encoding: 'utf-8' }).trim();
        return { branch, commit, summary: output };
    } catch (err) {
        return null;
    }
}

// ---------------------------------------
// Server
// ---------------------------------------
const server = createServer(async (req, res) => {
    // CORS preflight
    if (req.method === 'OPTIONS') {
        res.writeHead(200, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization'
        });
        return res.end();
    }

    const url = new URL(req.url, `http://${req.headers.host}`);
    const pathname = url.pathname;

    // ---------------------------------------
    // /api/ping
    // ---------------------------------------
    if (pathname === '/api/ping' && req.method === 'GET') {
        return sendJSON(res, { ok: true, message: 'pong' });
    }

    // ---------------------------------------
    // /api/generate
    // ---------------------------------------
    if (pathname === '/api/generate' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const config = JSON.parse(body);
                const urlObj = new URL(req.url, `http://${req.headers.host}`);
                const exportAsConfig = urlObj.searchParams.get("exportAsConfig") === "true";
                let fileName = "config.json";

                if (exportAsConfig) {
                    fileName = "config.json";
                } else if (config.context) {
                    fileName = slugify(config.context) + ".json";
                }

                writeFileSync(join(ROOT, fileName), JSON.stringify(config, null, 2));
                console.log(`✅ ${fileName} written to ${ROOT}`);

                // Read or initialize index.json
                const slice = config.slices.find(it => it.title)
                const baseFolder = join(SLICES_DIR, slice.context ?? "default");

                if (!existsSync(baseFolder)) {
                    mkdirSync(baseFolder, { recursive: true });
                }
                writeFileSync(join(baseFolder, 'config.json'), JSON.stringify(config, null, 2));
                console.log(`✅ config.json written to ${baseFolder}`);

                const contextName = slice.context ?? "default";
                writeFileSync(join(SLICES_DIR, 'current_context.json'), JSON.stringify({ name: contextName }, null, 2));
                console.log(`✅ current_context.json updated with context: ${contextName}`);

                const indexFile = join(baseFolder, 'index.json');
                let sliceIndices = { slices: [] };
                if (existsSync(indexFile)) {
                    try {
                        sliceIndices = JSON.parse(readFileSync(indexFile, 'utf-8'));
                    } catch {
                        sliceIndices = { slices: [] };
                    }
                }

                // Write all slices
                if (config.slices) {
                    // Build a map of slice ID to group ID from sliceGroups
                    const sliceToGroupMap = new Map();
                    if (config.sliceGroups) {
                        config.sliceGroups.forEach((group) => {
                            if (group.sliceIds) {
                                group.sliceIds.forEach((sliceId) => {
                                    sliceToGroupMap.set(sliceId, group.id);
                                });
                            }
                        });
                    }

                    config.slices.forEach((slice) => {
                        const sliceFolder = slice.title?.replaceAll(" ", "")?.replaceAll("slice:", "")?.toLowerCase();
                        const folder = join(baseFolder, sliceFolder);

                        if (!existsSync(folder)) {
                            mkdirSync(folder, { recursive: true });
                        }

                        const filePath = join(folder, 'slice.json');
                        const sliceData = { ...slice };
                        delete sliceData.index;

                        // Add group ID to slice data if it belongs to a group
                        const groupId = sliceToGroupMap.get(slice.id);
                        if (groupId) {
                            sliceData.group = groupId;
                        }

                        writeFileSync(filePath, JSON.stringify(sliceData, null, 2));

                        const sliceIndex = {
                            id: slice.id,
                            slice: slice.title,
                            index: slice.index,
                            context: slice.context ?? "default",
                            folder: sliceFolder,
                            status: slice.status,
                        };
                        // Add group ID to index entry if it belongs to a group
                        if (groupId) {
                            sliceIndex.group = groupId;
                        }

                        const sliceId = sliceIndices.slices.findIndex(it => it.slice == slice.title);
                        if (sliceId == -1) {
                            sliceIndices.slices.push(sliceIndex);
                        } else {
                            sliceIndices.slices[sliceId] = {...sliceIndices.slices[sliceId], ...sliceIndex, assigned: undefined};
                        }
                    });
                    writeFileSync(indexFile, JSON.stringify(sliceIndices, null, 2));

                    // Write context.json
                    const contextFile = join(baseFolder, 'context.json');
                    const contextData = {
                        name: config.context,
                        contextPackage: config.codeGen?.contextPackage
                    };
                    writeFileSync(contextFile, JSON.stringify(contextData, null, 2));
                    console.log(`✅ context.json written to ${baseFolder}`);
                }

                // Write slice images
                if (config.sliceImages) {
                    config.sliceImages.forEach((sliceImage) => {
                        const sliceFolder = sliceImage.slice?.replaceAll(" ", "")?.replaceAll("slice:", "")?.toLowerCase();
                        const folder = join(SLICES_DIR, sliceImage.context ?? "default", sliceFolder);

                        if (!existsSync(folder)) {
                            mkdirSync(folder, { recursive: true });
                        }

                        const base64String = sliceImage.base64Image.replace(/^data:image\/[a-z]+;base64,/, '');
                        const buffer = Buffer.from(base64String, 'base64');
                        const filePath = join(folder, `screen-${sliceImage.id}.png`);
                        writeFileSync(filePath, buffer);
                    });
                }

                sendJSON(res, { success: true, path: join(ROOT, fileName) });
            } catch (err) {
                sendJSON(res, { success: false, error: err.message }, 400);
            }
        });
        return;
    }

    // ---------------------------------------
    // /api/slice-info
    // ---------------------------------------
    if (pathname === '/api/slice-info' && req.method === 'GET') {
        try {
            const currentContextPath = join(SLICES_DIR, 'current_context.json');
            let contextName = null;
            if (existsSync(currentContextPath)) {
                contextName = JSON.parse(readFileSync(currentContextPath, 'utf-8')).name;
            }
            const indexPath = contextName
                ? join(SLICES_DIR, contextName, 'index.json')
                : join(SLICES_DIR, 'index.json');
            if (!existsSync(indexPath)) return sendJSON(res, { error: 'index.json not found' }, 404);
            const indexData = JSON.parse(readFileSync(indexPath, 'utf-8'));
            const specificationsMap = new Map();

            const searchBase = contextName ? join(SLICES_DIR, contextName) : SLICES_DIR;
            const codeFiles = findFilesRecursive(searchBase, name => name === 'code-slice.json');
            for (const file of codeFiles) {
                try {
                    const cs = JSON.parse(readFileSync(file, 'utf-8'));
                    if (cs.id && cs.specifications) specificationsMap.set(cs.id, cs.specifications);
                } catch {}
            }

            const allSlices = indexData.slices.map(s => ({
                title: s.slice,
                status: s.status,
                assigned: s.assigned,
                id: s.id,
                specifications: specificationsMap.get(s.id)
            }));

            return sendJSON(res, { slices: allSlices });
        } catch (err) {
            return sendJSON(res, { error: 'Failed to load slice-info', message: err.message }, 500);
        }
    }

    // ---------------------------------------
    // /api/slicepath
    // ---------------------------------------
    if (pathname === '/api/slicepath' && req.method === 'GET') {
        try {
            const sliceFiles = findFilesRecursive(SLICES_DIR, name => name.endsWith('.slice.json'));
            const slices = sliceFiles.map(f => ({
                path: relative(ROOT, f),
                name: f.split('/').pop()
            }));
            return sendJSON(res, { slices });
        } catch (err) {
            return sendJSON(res, { error: 'Failed to list slice paths', message: err.message }, 500);
        }
    }

    // ---------------------------------------
    // /api/slices
    // ---------------------------------------
    if (pathname === '/api/slices' && req.method === 'GET') {
        try {
            const includeImages = url.searchParams.get('includeImages') === 'true';
            const revision = url.searchParams.get('revision') ?? 'HEAD';
            const isHEAD = revision === 'HEAD';
            const isRepo = isGitRepo(repoPath);

            // slice.json files
            const trackedFiles = isRepo ? gitLsTree(repoPath, revision, '.slices') : [];
            const sliceFiles = trackedFiles.filter(f => f.endsWith('slice.json'));
            const trackedScreens = trackedFiles.filter(f => f.match(/screen-\d+\.png$/));

            // filesystem screens if HEAD
            let allScreens = [];
            if (isHEAD) {
                const fsScreens = findFilesRecursive(SLICES_DIR, name => name.match(/screen-\d+\.png$/));
                allScreens = [...new Set([...fsScreens, ...trackedScreens])];
            } else allScreens = trackedScreens;

            const slices = [];
            for (const file of sliceFiles) {
                if (isHEAD && existsSync(join(ROOT, file))) {
                    slices.push(JSON.parse(readFileSync(join(ROOT, file), 'utf-8')));
                } else if (isRepo) {
                    const content = gitShow(repoPath, `${revision}:${file}`);
                    if (content) slices.push(JSON.parse(content));
                }
            }

            const sliceImages = [];
            if (includeImages) {
                for (const screenPath of allScreens) {
                    try {
                        const buffer = isHEAD && existsSync(screenPath)
                            ? readFileSync(screenPath)
                            : execSync(`git show ${revision}:${screenPath}`, { cwd: ROOT, encoding: 'buffer' });

                        sliceImages.push({
                            id: parseIdWithRegex(screenPath.split('/').pop()) ?? '',
                            slice: '',
                            title: '',
                            base64Image: `data:image/png;base64,${buffer.toString('base64')}`
                        });
                    } catch {}
                }
            }

            return sendJSON(res, { slices, sliceImages, meta: { revision, sliceCount: slices.length, screenCount: sliceImages.length } });
        } catch (err) {
            return sendJSON(res, { error: 'Failed to load slices', message: err.message }, 500);
        }
    }

    // ---------------------------------------
    // /api/config
    // ---------------------------------------
    if (pathname === '/api/config' && req.method === 'GET') {
        const storedData = { version: 1.0 };
        try {
            const gitRepo = isGitRepo(repoPath);
            if (storedData) storedData.gitRepo = gitRepo;
        } catch (e) {}
        return sendJSON(res, storedData);
    }

    if (pathname === '/api/config' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                JSON.parse(body);
                return sendJSON(res, { success: 'Data stored successfully!' });
            } catch (err) {
                return sendJSON(res, { error: 'Failed to store data' }, 500);
            }
        });
        return;
    }

    // ---------------------------------------
    // /api/progress
    // ---------------------------------------
    if (pathname === '/api/progress' && req.method === 'GET') {
        try {
            const progressPath = join(repoPath, 'progress.txt');
            if (!existsSync(progressPath)) {
                return sendJSON(res, { available: false, progress: [] });
            }
            const content = readFileSync(progressPath, 'utf-8');
            const paragraphs = content
                .split(/(?=>>> Iteration \d+)/)
                .map(p => p.trim())
                .filter(p => p.length > 0);
            return sendJSON(res, { available: true, progress: paragraphs });
        } catch (err) {
            return sendJSON(res, { error: 'Failed to load progress', message: err.message }, 500);
        }
    }

    // ---------------------------------------
    // /api/delete-slice
    // ---------------------------------------
    if (pathname === '/api/delete-slice' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const sliceId = data.id;

                if (!sliceId) {
                    return sendJSON(res, { error: 'Missing required parameter: id' }, 400);
                }

                function findAndDeleteCodeSliceById(dir, targetId) {
                    if (!existsSync(dir)) return { found: false };
                    const entries = readdirSync(dir, { withFileTypes: true });

                    for (const entry of entries) {
                        const fullPath = join(dir, entry.name);
                        if (entry.isDirectory()) {
                            const result = findAndDeleteCodeSliceById(fullPath, targetId);
                            if (result.found) return result;
                        } else if (entry.name === 'code-slice.json') {
                            try {
                                const codeSliceContent = readFileSync(fullPath, 'utf-8');
                                const codeSlice = JSON.parse(codeSliceContent);
                                if (codeSlice.id === targetId) {
                                    unlinkSync(fullPath);
                                    return { found: true, deletedPath: fullPath };
                                }
                            } catch (error) {
                                console.error(`Error reading code-slice.json at ${fullPath}:`, error);
                            }
                        }
                    }
                    return { found: false };
                }

                if (!existsSync(SLICES_DIR)) {
                    return sendJSON(res, { error: '.slices directory not found' }, 404);
                }

                const result = findAndDeleteCodeSliceById(SLICES_DIR, sliceId);
                if (result.found) {
                    return sendJSON(res, {
                        success: true,
                        message: `Successfully deleted code-slice.json with id: ${sliceId}`,
                        deletedPath: result.deletedPath
                    });
                } else {
                    return sendJSON(res, { error: `No code-slice.json file found with id: ${sliceId}` }, 404);
                }
            } catch (err) {
                return sendJSON(res, { error: 'Failed to delete code-slice.json', message: err.message }, 500);
            }
        });
        return;
    }

    // ---------------------------------------
    // /api/git
    // ---------------------------------------
    if (pathname === '/api/git' && req.method === 'GET') {
        const storedData = { version: 1.0 };
        try {
            const gitRepo = isGitRepo(repoPath);
            if (storedData) storedData.gitRepo = gitRepo;
        } catch (e) {}
        return sendJSON(res, storedData);
    }

    if (pathname === '/api/git' && req.method === 'POST') {
        try {
            let isRepo = isGitRepo(repoPath);
            if (!isRepo) {
                gitInit(repoPath);
            }
            gitAdd(repoPath, '.slices');
            const statusResult = gitStatus(repoPath);

            let commitResult = undefined;
            if (statusResult.staged?.length > 0) {
                commitResult = gitCommit(repoPath, '(chore) slices', '.slices');
            }

            return sendJSON(res, {
                branch: commitResult?.branch,
                revision: commitResult?.commit
            });
        } catch (error) {
            console.error('Error committing:', error);
            return sendJSON(res, {
                error: 'Failed to commit slices',
                message: error instanceof Error ? error.message : 'Unknown error'
            }, 500);
        }
    }

    // ---------------------------------------
    // 404
    // ---------------------------------------
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
});

// ---------------------------------------
// Listen
// ---------------------------------------
const port = parseInt(process.env.PORT || '3001', 10);
server.listen(port, () => {
    console.log(`🚀 Server running at http://localhost:${port}`);
    console.log(`📁 ROOT:       ${ROOT}`);
    console.log(`📁 SLICES_DIR: ${SLICES_DIR}`);
    console.log(`📁 repoPath:   ${repoPath}`);
    console.log('💓 GET /api/ping');
    console.log('📥 POST /api/generate');
    console.log('📄 GET /api/slice-info');
    console.log('📂 GET /api/slicepath');
    console.log('📦 GET /api/slices?includeImages=true');
    console.log('⚙️  GET/POST /api/config');
    console.log('📊 GET /api/progress');
    console.log('🗑️  POST /api/delete-slice');
    console.log('🔧 GET/POST /api/git');
});
