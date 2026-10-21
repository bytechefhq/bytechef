import {spawnSync} from 'node:child_process';
import {existsSync, mkdtempSync, readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {describe, expect, it} from 'vitest';

const bin = resolve(dirname(fileURLToPath(import.meta.url)), '..', 'bin', 'create-bytechef-app.mjs');

function run(args, extraEnv = {}) {
    const env = {...process.env};

    for (const name of Object.keys(env)) {
        if (name.startsWith('npm_config_') || name.startsWith('BYTECHEF_')) {
            delete env[name];
        }
    }

    const cwd = mkdtempSync(join(tmpdir(), 'cba-bin-'));

    return {
        ...spawnSync(process.execPath, [bin, ...args], {cwd, encoding: 'utf8', env: {...env, ...extraEnv}}),
        cwd,
    };
}

describe('create-bytechef-app bin', () => {
    it('scaffolds, writes the key from the environment to .env.local and never prints it', () => {
        const parent = mkdtempSync(join(tmpdir(), 'cba-bin-out-'));
        const target = join(parent, 'app');
        const result = run([target, '--url', 'https://bc.example', '--no-install'], {BYTECHEF_APP_KEY: 'sk-test'});

        expect(result.status).toBe(0);
        expect(result.stdout).toContain('BYTECHEF_APP_KEY taken from the environment');
        expect(result.stdout).not.toContain('sk-test');
        expect(result.stderr).not.toContain('sk-test');
        expect(existsSync(join(target, 'package.json'))).toBe(true);
        expect(readFileSync(join(target, '.env.local'), 'utf8')).toBe(
            'BYTECHEF_URL=https://bc.example\nBYTECHEF_APP_KEY=sk-test\n'
        );
    });

    it('prints the usage and exits 0 on --help and -h without creating anything', () => {
        for (const flag of ['--help', '-h']) {
            const result = run([flag]);

            expect(result.status).toBe(0);
            expect(result.stdout).toContain('npm create bytechef-app@latest [directory] -- [flags]');
            expect(result.stdout).toContain('pnpm create bytechef-app [directory] [flags]');
            expect(result.stdout).toContain('There is no flag for the app key');
            expect(result.stdout).not.toContain('Created');
            expect(existsSync(join(result.cwd, 'bytechef-app'))).toBe(false);
        }
    });

    it('prints the package-manager-specific status command in the next steps', () => {
        const parent = mkdtempSync(join(tmpdir(), 'cba-bin-out-'));
        const npm = run([join(parent, 'npm-app'), '--no-install', '--package-manager', 'npm']);
        const yarn = run([join(parent, 'yarn-app'), '--no-install', '--package-manager', 'yarn']);

        expect(npm.status).toBe(0);
        expect(npm.stdout).toContain('npm run -s bytechef:status');
        expect(yarn.status).toBe(0);
        expect(yarn.stdout).toContain('yarn run bytechef:status');
        expect(yarn.stdout).not.toContain('-s bytechef:status');
    });

    it('keeps package-lock.json for npm and removes it, with one line, for other package managers', () => {
        const parent = mkdtempSync(join(tmpdir(), 'cba-bin-out-'));
        const npm = run([join(parent, 'npm-app'), '--no-install', '--package-manager', 'npm']);
        const pnpm = run([join(parent, 'pnpm-app'), '--no-install', '--package-manager', 'pnpm']);

        expect(npm.status).toBe(0);
        expect(existsSync(join(parent, 'npm-app', 'package-lock.json'))).toBe(true);
        expect(npm.stdout).not.toContain('Removed package-lock.json');
        expect(pnpm.status).toBe(0);
        expect(existsSync(join(parent, 'pnpm-app', 'package-lock.json'))).toBe(false);
        expect(pnpm.stdout).toContain(
            'Removed package-lock.json: pnpm resolves transitive dependencies fresh; npm installs the locked versions.\n'
        );
    });

    it('tells a --no-install scaffold to install before deploying', () => {
        const parent = mkdtempSync(join(tmpdir(), 'cba-bin-out-'));
        const result = run([join(parent, 'app'), '--no-install', '--package-manager', 'yarn']);

        expect(result.status).toBe(0);
        expect(result.stdout).toContain(
            'Run yarn install before deploying: the lockfile gains @bytechef/app on the first install.'
        );
    });

    it('exits 2 with the npm -- hint on a second positional', () => {
        const result = run(['one', 'two']);

        expect(result.status).toBe(2);
        expect(result.stderr).toContain('npm create bytechef-app@latest my-app -- --url');
    });

    it('creates name/ for @scope/name with the scoped package name', () => {
        const result = run(['@acme/my-app', '--no-install']);

        expect(result.status).toBe(0);
        expect(existsSync(join(result.cwd, '@acme'))).toBe(false);
        expect(JSON.parse(readFileSync(join(result.cwd, 'my-app', 'package.json'), 'utf8')).name).toBe('@acme/my-app');
        expect(result.stdout).toContain('cd my-app\n');
    });

    it('refuses a key with a line break before writing anything and never prints the key', () => {
        const result = run(['app', '--no-install'], {BYTECHEF_APP_KEY: 'sk-secret\rINJECT=1'});

        expect(result.status).toBe(1);
        expect(result.stderr).toContain('BYTECHEF_APP_KEY must not contain a line break');
        expect(result.stderr).not.toContain('sk-secret');
        expect(result.stdout).not.toContain('sk-secret');
        expect(existsSync(join(result.cwd, 'app'))).toBe(false);
    });

    it('exits 2 on a --url that is not http or https', () => {
        const result = run(['app', '--url', 'ftp://bc.example', '--no-install']);

        expect(result.status).toBe(2);
        expect(result.stderr).toContain('--url must be an http or https URL');
        expect(existsSync(join(result.cwd, 'app'))).toBe(false);
    });
});
