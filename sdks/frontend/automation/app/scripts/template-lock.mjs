import {spawnSync} from 'node:child_process';
import {copyFileSync, mkdtempSync, readFileSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, resolve} from 'node:path';

const TEMPLATE = resolve(import.meta.dirname, '../create/template');
const PUBLIC_REGISTRY = 'https://registry.npmjs.org/';
const childEnv = Object.fromEntries(
    Object.entries(process.env).filter(([name]) => name.toLowerCase() !== 'npm_config_registry')
);
const work = mkdtempSync(join(tmpdir(), 'bytechef-template-lock-'));

try {
    const packageJson = JSON.parse(readFileSync(join(TEMPLATE, 'package.json'), 'utf8'));

    delete packageJson.dependencies['@bytechef/app'];

    writeFileSync(join(work, 'package.json'), `${JSON.stringify(packageJson, null, 4)}\n`);

    for (const command of ['install', 'prune']) {
        const args = [command, '--ignore-scripts', '--no-audit', '--no-fund', `--registry=${PUBLIC_REGISTRY}`];
        const result = spawnSync('npm', args, {
            cwd: work,
            env: childEnv,
            shell: process.platform === 'win32',
            stdio: 'inherit',
        });

        if (result.status !== 0) {
            throw new Error(`npm ${args.join(' ')} failed (exit ${result.status ?? result.error?.message})`);
        }
    }

    copyFileSync(join(work, 'package-lock.json'), join(TEMPLATE, 'package-lock.json'));

    console.log(`Wrote ${join(TEMPLATE, 'package-lock.json')}`);
} catch (error) {
    console.error(error.message);
    process.exitCode = 1;
} finally {
    rmSync(work, {force: true, recursive: true});
}
