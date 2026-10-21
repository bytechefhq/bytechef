import {chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, statSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {describe, expect, it} from 'vitest';

import {lockfileRemovedMessage, scaffold, writeEnvLocal} from '../src/scaffold.mjs';

function makeTemplate() {
    const dir = mkdtempSync(join(tmpdir(), 'cba-template-'));

    mkdirSync(join(dir, 'src'));
    writeFileSync(join(dir, '_gitignore'), 'node_modules\n');
    writeFileSync(join(dir, '_dockerignore'), 'node_modules\n');
    writeFileSync(
        join(dir, 'package.json'),
        JSON.stringify({dependencies: {'@bytechef/app': '0.1.0'}, name: 'bytechef-app'}, null, 4)
    );
    writeFileSync(join(dir, 'src', 'x.ts'), 'export const x = 1;\n');
    writeFileSync(join(dir, 'package-lock.json'), '{}\n');

    return dir;
}

describe('scaffold', () => {
    it('copies the template, renames the dotfiles, rewrites package.json and writes .env.local with mode 0600', () => {
        const target = join(mkdtempSync(join(tmpdir(), 'cba-target-')), 'my-app');

        scaffold({
            appVersion: '0.1.0',
            envLocal: 'BYTECHEF_URL=\nBYTECHEF_APP_KEY=\n',
            packageName: 'my-app',
            targetDir: target,
            templateDir: makeTemplate(),
        });

        expect(readFileSync(join(target, '.gitignore'), 'utf8')).toBe('node_modules\n');
        expect(readFileSync(join(target, '.dockerignore'), 'utf8')).toBe('node_modules\n');
        expect(statSync(join(target, '_gitignore'), {throwIfNoEntry: false})).toBeUndefined();
        expect(JSON.parse(readFileSync(join(target, 'package.json'), 'utf8'))).toEqual({
            dependencies: {'@bytechef/app': '0.1.0'},
            name: 'my-app',
        });
        expect(readFileSync(join(target, '.env.local'), 'utf8')).toBe('BYTECHEF_URL=\nBYTECHEF_APP_KEY=\n');

        if (process.platform !== 'win32') {
            expect(statSync(join(target, '.env.local')).mode & 0o777).toBe(0o600);
        }

        expect(readFileSync(join(target, 'src', 'x.ts'), 'utf8')).toBe('export const x = 1;\n');
    });

    it('refuses a non-empty target directory by name', () => {
        const target = mkdtempSync(join(tmpdir(), 'cba-busy-'));

        writeFileSync(join(target, 'keep.txt'), 'x');

        expect(() =>
            scaffold({
                appVersion: '0.1.0',
                envLocal: '',
                packageName: 'busy',
                targetDir: target,
                templateDir: makeTemplate(),
            })
        ).toThrowError(new RegExp(`${target}.*not empty`));
    });

    it('pins @bytechef/app to the given appVersion', () => {
        const target = join(mkdtempSync(join(tmpdir(), 'cba-pin-')), 'pinned');

        scaffold({
            appVersion: '9.9.9',
            envLocal: '',
            packageName: 'pinned',
            targetDir: target,
            templateDir: makeTemplate(),
        });

        expect(JSON.parse(readFileSync(join(target, 'package.json'), 'utf8')).dependencies['@bytechef/app']).toBe(
            '9.9.9'
        );
    });

    it('does not copy local build output, dependencies or secrets from the template', () => {
        const template = makeTemplate();
        const target = join(mkdtempSync(join(tmpdir(), 'cba-clean-')), 'clean');

        for (const directory of ['node_modules', '.output', 'dist', '.tanstack', '.vercel', '.nitro']) {
            mkdirSync(join(template, directory));
            writeFileSync(join(template, directory, 'x'), 'x');
        }

        for (const file of ['.env', '.env.local', '.env.production.local']) {
            writeFileSync(join(template, file), 'BYTECHEF_APP_KEY=leaked\n');
        }

        writeFileSync(join(template, '.env.example'), 'BYTECHEF_URL=\n');

        scaffold({
            appVersion: '0.1.0',
            envLocal: 'A=1\n',
            packageName: 'clean',
            targetDir: target,
            templateDir: template,
        });

        for (const name of [
            'node_modules',
            '.output',
            'dist',
            '.tanstack',
            '.vercel',
            '.nitro',
            '.env',
            '.env.production.local',
        ]) {
            expect(existsSync(join(target, name))).toBe(false);
        }

        expect(readFileSync(join(target, '.env.local'), 'utf8')).toBe('A=1\n');
        expect(existsSync(join(target, '.env.example'))).toBe(true);
    });

    it('keeps package-lock.json for npm and removes it for any other package manager', () => {
        const parent = mkdtempSync(join(tmpdir(), 'cba-lock-'));
        const results = Object.fromEntries(
            ['npm', 'pnpm', 'yarn', 'bun'].map((packageManager) => [
                packageManager,
                scaffold({
                    appVersion: '0.1.0',
                    envLocal: '',
                    packageManager,
                    packageName: packageManager,
                    targetDir: join(parent, packageManager),
                    templateDir: makeTemplate(),
                }),
            ])
        );

        expect(results.npm).toEqual({lockfileRemoved: false});
        expect(readFileSync(join(parent, 'npm', 'package-lock.json'), 'utf8')).toBe('{}\n');

        for (const packageManager of ['pnpm', 'yarn', 'bun']) {
            expect(results[packageManager]).toEqual({lockfileRemoved: true});
            expect(existsSync(join(parent, packageManager, 'package-lock.json'))).toBe(false);
        }
    });

    it('names the package manager in the lockfile notice', () => {
        expect(lockfileRemovedMessage('pnpm')).toBe(
            'Removed package-lock.json: pnpm resolves transitive dependencies fresh; npm installs the locked versions.'
        );
    });

    it('refuses a target that is a file', () => {
        const target = join(mkdtempSync(join(tmpdir(), 'cba-file-')), 'a-file');

        writeFileSync(target, 'x');

        expect(() =>
            scaffold({
                appVersion: '0.1.0',
                envLocal: '',
                packageName: 'a-file',
                targetDir: target,
                templateDir: makeTemplate(),
            })
        ).toThrowError(new RegExp(`${target} exists and is not a directory`));
    });

    it('accepts an existing empty target directory', () => {
        const target = mkdtempSync(join(tmpdir(), 'cba-empty-'));

        scaffold({
            appVersion: '0.1.0',
            envLocal: 'A=1\n',
            packageName: 'empty',
            targetDir: target,
            templateDir: makeTemplate(),
        });

        expect(readFileSync(join(target, 'src', 'x.ts'), 'utf8')).toBe('export const x = 1;\n');
    });
});

describe('writeEnvLocal', () => {
    it('leaves .env.local with mode 0600 even when the file already existed with wider permissions', () => {
        const target = mkdtempSync(join(tmpdir(), 'cba-env-'));
        const path = join(target, '.env.local');

        writeFileSync(path, 'old', {mode: 0o644});
        chmodSync(path, 0o644);

        writeEnvLocal(target, 'BYTECHEF_APP_KEY=sk-1\n');

        expect(readFileSync(path, 'utf8')).toBe('BYTECHEF_APP_KEY=sk-1\n');

        if (process.platform !== 'win32') {
            expect(statSync(path).mode & 0o777).toBe(0o600);
        }
    });
});
