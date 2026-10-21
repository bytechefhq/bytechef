import {describe, expect, it} from 'vitest';

import {createCodeChallenge, generateCodeVerifier, generateState, isSameState} from './pkce';

const UNRESERVED = /^[A-Za-z0-9\-._~]+$/;

describe('pkce', () => {
    it('derives the RFC 7636 appendix B challenge from its verifier', async () => {
        await expect(createCodeChallenge('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk')).resolves.toBe(
            'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM'
        );
    });

    it('generates verifiers of 43 unreserved characters that differ on every call', () => {
        const first = generateCodeVerifier();
        const second = generateCodeVerifier();

        expect(first).toMatch(UNRESERVED);
        expect(first).toHaveLength(43);
        expect(second).not.toBe(first);
    });

    it('generates url-safe states that differ on every call', () => {
        const first = generateState();

        expect(first).toMatch(UNRESERVED);
        expect(first.length).toBeGreaterThanOrEqual(43);
        expect(generateState()).not.toBe(first);
    });

    it('refuses a verifier outside 43 to 128 unreserved characters', async () => {
        await expect(createCodeChallenge('short')).rejects.toBeInstanceOf(RangeError);
        await expect(createCodeChallenge('a'.repeat(129))).rejects.toBeInstanceOf(RangeError);
        await expect(createCodeChallenge(`${'a'.repeat(42)}+`)).rejects.toBeInstanceOf(RangeError);
    });

    it('compares the expected and the returned state', () => {
        const state = generateState();

        expect(isSameState(state, state)).toBe(true);
        expect(isSameState(state, `${state.slice(0, -1)}${state.endsWith('A') ? 'B' : 'A'}`)).toBe(false);
        expect(isSameState(state, state.slice(1))).toBe(false);
        expect(isSameState(state, `${state}A`)).toBe(false);
        expect(isSameState(state, null)).toBe(false);
        expect(isSameState(state, undefined)).toBe(false);
        expect(isSameState('', '')).toBe(false);
    });
});
