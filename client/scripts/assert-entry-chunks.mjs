// Guards the embedded-builder fast-boot invariant: neither the main app entry ('index.html') nor
// the dedicated workflow-builder entry ('workflow-builder.html') may STATICALLY reach the heavy
// Monaco, assistant-ui, or posthog chunks. Those packages must only be pulled in through dynamic
// `import()` boundaries (React.lazy, etc.) so the fast-boot entries stay small on first paint.
//
// This walks only the manifest's "imports" edges (static ESM imports emitted by Rollup/Vite),
// never "dynamicImports" — a chunk that is only reachable via a dynamic import is, by definition,
// not part of the static/eager graph and is exactly what we want to allow.

import {readFile} from 'node:fs/promises';
import * as path from 'node:path';
import {fileURLToPath} from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const MANIFEST_PATH = path.resolve(__dirname, '../dist/.vite/manifest.json');
const DIST_PATH = path.resolve(__dirname, '../dist');

const ENTRY_KEYS = ['index.html', 'workflow-builder.html'];

const FORBIDDEN_CONTENT_PATTERN = /monaco-editor|assistant-ui|posthog-js/;

async function loadManifest() {
    const raw = await readFile(MANIFEST_PATH, 'utf-8');

    return JSON.parse(raw);
}

// Walks static imports only (never dynamicImports) starting from `entryKey`, returning the set of
// manifest keys reachable that way, along with the first offending chunk if one matches the
// forbidden pattern (by content).
async function walkStaticImports(manifest, entryKey) {
    const visited = new Set();
    const stack = [entryKey];
    let offender;
    let candidateCount = 0;
    let scannedCount = 0;
    const failures = [];

    while (stack.length > 0) {
        const key = stack.pop();

        if (visited.has(key)) {
            continue;
        }

        visited.add(key);

        const chunk = manifest[key];

        if (!chunk) {
            continue;
        }

        if (chunk.file) {
            candidateCount++;

            if (!offender) {
                const chunkPath = path.resolve(DIST_PATH, chunk.file);

                try {
                    const content = await readFile(chunkPath, 'utf-8');
                    const match = content.match(FORBIDDEN_CONTENT_PATTERN);

                    scannedCount++;

                    if (match) {
                        offender = {chunkFile: chunk.file, chunkKey: key, pattern: match[0]};
                    }
                } catch (error) {
                    failures.push({chunkFile: chunk.file, chunkKey: key, chunkPath, error});
                }
            }
        }

        for (const importedKey of chunk.imports ?? []) {
            if (!visited.has(importedKey)) {
                stack.push(importedKey);
            }
        }
    }

    return {candidateCount, failures, offender, scannedCount, visited};
}

async function main() {
    const manifest = await loadManifest();

    const results = [];

    for (const entryKey of ENTRY_KEYS) {
        if (!manifest[entryKey]) {
            console.error(`Entry "${entryKey}" not found in manifest at ${MANIFEST_PATH}.`);

            process.exit(1);

            return;
        }

        const {candidateCount, failures, offender, scannedCount} = await walkStaticImports(manifest, entryKey);

        if (offender) {
            console.error(
                `Entry "${entryKey}" statically reaches a forbidden chunk: "${offender.chunkFile}" ` +
                    `(manifest key "${offender.chunkKey}", content match: ${offender.pattern}).`
            );
            console.error(
                'Monaco, assistant-ui, and posthog must only be reachable via a dynamic import() ' +
                    '(React.lazy, etc.) from these fast-boot entries.'
            );

            process.exit(1);

            return;
        }

        if (failures.length > 0) {
            console.error(
                `Entry "${entryKey}": assertion cannot run: ${failures.length} of ${candidateCount} chunks unreadable.`
            );

            for (const failure of failures.slice(0, 5)) {
                console.error(
                    `  - ${failure.chunkFile} (manifest key "${failure.chunkKey}", path: ${failure.chunkPath}): ` +
                        `${failure.error.message}`
                );
            }

            process.exit(1);

            return;
        }

        if (scannedCount !== candidateCount) {
            console.error(
                `Entry "${entryKey}": scanned ${scannedCount} chunks but expected ${candidateCount} ` +
                    '(scanned-chunk count mismatch) — assertion cannot be trusted.'
            );

            process.exit(1);

            return;
        }

        results.push({entryKey, scannedCount});
    }

    for (const {entryKey, scannedCount} of results) {
        console.log(`${entryKey}: ${scannedCount} chunks scanned, none forbidden.`);
    }

    process.exit(0);
}

main().catch((error) => {
    console.error(error);

    process.exit(1);
});
