import {beforeEach, describe, expect, it} from 'vitest';

import {buildLoginPath, consumeLoginRedirect, getLoginRedirect, rememberLoginRedirect} from '../login-redirect-utils';

describe('buildLoginPath', () => {
    it('records the requested location so it survives a full page load', () => {
        expect(buildLoginPath({pathname: '/automation/projects/1052/templates/ai-email-classifier'})).toBe(
            '/login?redirect=%2Fautomation%2Fprojects%2F1052%2Ftemplates%2Fai-email-classifier'
        );
    });

    it('keeps the query string and hash of the requested location', () => {
        expect(buildLoginPath({hash: '#tab', pathname: '/automation/projects', search: '?environment=2'})).toBe(
            '/login?redirect=%2Fautomation%2Fprojects%3Fenvironment%3D2%23tab'
        );
    });

    it('records nothing for the landing page', () => {
        expect(buildLoginPath({pathname: '/'})).toBe('/login');
    });

    it('records nothing when already on the login page', () => {
        expect(buildLoginPath({pathname: '/login'})).toBe('/login');
    });
});

describe('getLoginRedirect', () => {
    it('reads back a recorded location', () => {
        expect(getLoginRedirect('?redirect=%2Fautomation%2Fprojects%3Fenvironment%3D2')).toBe(
            '/automation/projects?environment=2'
        );
    });

    it('returns undefined when nothing was recorded', () => {
        expect(getLoginRedirect('?company=acme')).toBeUndefined();
    });

    it('rejects an absolute url so a crafted link cannot bounce the user off site', () => {
        expect(getLoginRedirect(`?redirect=${encodeURIComponent('https://evil.example')}`)).toBeUndefined();
    });

    it('rejects a protocol-relative url', () => {
        expect(getLoginRedirect(`?redirect=${encodeURIComponent('//evil.example')}`)).toBeUndefined();
    });

    it('rejects a backslash-escaped protocol-relative url', () => {
        expect(getLoginRedirect(`?redirect=${encodeURIComponent('/\\evil.example')}`)).toBeUndefined();
    });
});

describe('getLoginRedirect open-redirect rejection', () => {
    it.each([
        ['protocol-relative', '?redirect=%2F%2Fevil.example'],
        ['backslash-prefixed', '?redirect=%2F%5Cevil.example'],
        ['tab-obfuscated backslash', '?redirect=%2F%09%5Cevil.example'],
        ['newline-obfuscated backslash', '?redirect=%2F%0A%5Cevil.example'],
        ['carriage-return-obfuscated backslash', '?redirect=%2F%0D%5Cevil.example'],
        ['absolute url', '?redirect=https%3A%2F%2Fevil.example'],
        ['scheme-relative with credentials', '?redirect=%2F%2Fuser%40evil.example'],
    ])('rejects a %s target', (_label, search) => {
        expect(getLoginRedirect(search)).toBeUndefined();
    });

    it('keeps an ordinary same-origin path', () => {
        expect(getLoginRedirect('?redirect=%2Fautomation%2Fprojects')).toBe('/automation/projects');
    });
});

describe('rememberLoginRedirect / consumeLoginRedirect', () => {
    beforeEach(() => {
        window.sessionStorage.clear();
    });

    it('carries a destination across an external authentication round trip', () => {
        rememberLoginRedirect('/automation/projects/1052');

        expect(consumeLoginRedirect()).toBe('/automation/projects/1052');
    });

    it('yields the destination only once', () => {
        rememberLoginRedirect('/automation/projects/1052');
        consumeLoginRedirect();

        expect(consumeLoginRedirect()).toBeUndefined();
    });

    it('rejects a stored destination that is not same-origin', () => {
        window.sessionStorage.setItem('bytechef.loginRedirect', '/\u0009\\evil.example');

        expect(consumeLoginRedirect()).toBeUndefined();
    });

    it('clears the stored destination when given nothing', () => {
        rememberLoginRedirect('/automation/projects');
        rememberLoginRedirect(undefined);

        expect(consumeLoginRedirect()).toBeUndefined();
    });
});
