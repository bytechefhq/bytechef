import {readFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {describe, expect, it} from 'vitest';

const currentDirectory = dirname(fileURLToPath(import.meta.url));

const config = readFileSync(resolve(currentDirectory, '../../../tailwind.config.js'), 'utf8');
const stylesheet = readFileSync(resolve(currentDirectory, '../index.css'), 'utf8');

/**
 * Tokens that `tailwind.config.js` references via `var(...)` but that are never declared in
 * `index.css`, for reasons other than a missing declaration.
 *
 * The four `--radix-*` custom properties are set at runtime by Radix UI primitives themselves
 * (measured via ResizeObserver and written as inline styles on the trigger/content element, e.g.
 * `--radix-select-trigger-width`, `--radix-popper-anchor-width`,
 * `--radix-select-content-available-height`, `--radix-accordion-content-height`). They are
 * consumed by the config but deliberately never declared in our stylesheet.
 */
const CONFIG_EXEMPT_TOKENS = new Set([
    '--radix-accordion-content-height',
    '--radix-popper-anchor-width',
    '--radix-select-content-available-height',
    '--radix-select-trigger-width',
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

const extractConfigTokenReferences = (source: string): Set<string> =>
    new Set([...source.matchAll(/var\((--[a-z0-9-]+)\)/gi)].map((match) => match[1]));

describe('tailwind.config.js token resolution', () => {
    const rootTokens = extractTokenNames(extractBlockBody(stylesheet, ':root'));
    const configTokenReferences = extractConfigTokenReferences(config);

    it('resolves every config token to a declaration in index.css', () => {
        const unresolved = [...configTokenReferences]
            .filter((token) => !CONFIG_EXEMPT_TOKENS.has(token))
            .filter((token) => !rootTokens.has(token))
            .sort();

        expect(unresolved).toEqual([]);
    });
});
