import {readFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {describe, expect, it} from 'vitest';

const currentDirectory = dirname(fileURLToPath(import.meta.url));

const stylesheet = readFileSync(resolve(currentDirectory, '../index.css'), 'utf8');

const THEME_EXEMPT_TOKENS = new Set([
    '--hub-disable',
    '--hub-disable-hover',
    '--hub-enable',
    '--hub-enable-hover',
    '--hub-on-accent',
    '--radius',
    '--sidebar',
    '--sidebar-accent',
    '--sidebar-accent-foreground',
    '--sidebar-border',
    '--sidebar-foreground',
    '--sidebar-primary',
    '--sidebar-primary-foreground',
    '--sidebar-ring',
    '--workflow-nodes-popover-component-menu-width',
]);

const extractBlockBody = (source: string, selector: string): string => {
    const selectorIndex = source.indexOf(`${selector} {`);

    if (selectorIndex === -1) {
        throw new Error(`Could not find the "${selector}" block in index.css`);
    }

    const bodyStart = source.indexOf('{', selectorIndex) + 1;

    let depth = 0;

    for (let index = bodyStart - 1; index < source.length; index += 1) {
        if (source[index] === '{') {
            depth += 1;
        } else if (source[index] === '}') {
            depth -= 1;

            if (depth === 0) {
                return source.slice(bodyStart, index);
            }
        }
    }

    throw new Error(`The "${selector}" block in index.css is not terminated`);
};

const extractTokenNames = (blockBody: string): Set<string> =>
    new Set([...blockBody.matchAll(/^\s*(--[a-z0-9-_]+)\s*:/gim)].map((match) => match[1]));

describe('index.css theme token parity', () => {
    const rootTokens = extractTokenNames(extractBlockBody(stylesheet, ':root'));
    const darkTokens = extractTokenNames(extractBlockBody(stylesheet, '.dark'));

    it('defines a dark override for every themeable root token', () => {
        const missing = [...rootTokens]
            .filter((token) => !THEME_EXEMPT_TOKENS.has(token))
            .filter((token) => !darkTokens.has(token))
            .sort();

        expect(missing).toEqual([]);
    });

    it('does not define dark tokens that no longer exist in root', () => {
        const orphaned = [...darkTokens].filter((token) => !rootTokens.has(token)).sort();

        expect(orphaned).toEqual([]);
    });

    it('declares color-scheme in both themes so browser-native UI follows', () => {
        expect(extractBlockBody(stylesheet, ':root')).toMatch(/^\s*color-scheme:\s*light;?\s*$/m);
        expect(extractBlockBody(stylesheet, '.dark')).toMatch(/^\s*color-scheme:\s*dark;?\s*$/m);
    });
});
