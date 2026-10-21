import {readFileSync} from 'node:fs';

import {describe, expect, it} from 'vitest';

import {MINIMUM_TANSTACK_START_VERSION} from './tanstack/authMiddleware';

interface ManifestI {
    dependencies: Record<string, string>;
    devDependencies: Record<string, string>;
}

function compareVersions(left: string, right: string): number {
    const leftParts = left.split('.').map(Number);
    const rightParts = right.split('.').map(Number);

    for (let index = 0; index < 3; index++) {
        const difference = (leftParts[index] ?? 0) - (rightParts[index] ?? 0);

        if (difference !== 0) {
            return difference;
        }
    }

    return 0;
}

const templatePackageJson = JSON.parse(
    readFileSync(new URL('../../create/template/package.json', import.meta.url), 'utf8')
) as ManifestI;

const packageJson = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8')) as ManifestI & {
    exports: Record<string, unknown>;
    peerDependencies: Record<string, string>;
    peerDependenciesMeta: Record<string, {optional?: boolean}>;
};

describe('package.json', () => {
    it('keeps @tanstack/react-start as an optional peer, which makes Start compile the shipped server functions', () => {
        expect(packageJson.peerDependencies['@tanstack/react-start']).toBeDefined();
        expect(packageJson.peerDependenciesMeta['@tanstack/react-start']?.optional).toBe(true);
    });

    it('requires the first @tanstack/react-start that exports createCsrfMiddleware', () => {
        expect(packageJson.peerDependencies['@tanstack/react-start']).toBe(`^${MINIMUM_TANSTACK_START_VERSION}`);
    });

    it('is tested against, and scaffolds, a @tanstack/react-start inside the peer range', () => {
        const testedVersion = packageJson.devDependencies['@tanstack/react-start']!;
        const templateVersion = templatePackageJson.dependencies['@tanstack/react-start']!;

        for (const version of [testedVersion, templateVersion]) {
            expect(version).toMatch(/^1\.\d+\.\d+$/);
            expect(compareVersions(version, MINIMUM_TANSTACK_START_VERSION)).toBeGreaterThanOrEqual(0);
        }
    });

    it('keeps ai as an optional peer', () => {
        expect(packageJson.peerDependencies['ai']).toBeDefined();
        expect(packageJson.peerDependenciesMeta['ai']?.optional).toBe(true);
    });

    it('exports exactly the four entry points and package.json', () => {
        expect(Object.keys(packageJson.exports).sort()).toEqual([
            '.',
            './ai-sdk',
            './package.json',
            './tanstack-start',
            './tanstack-start/server-fns',
        ]);
    });
});
