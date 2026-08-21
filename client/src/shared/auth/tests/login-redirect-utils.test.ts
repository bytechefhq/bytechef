import {describe, expect, it} from 'vitest';

import {buildLoginPath, getLoginRedirect} from '../login-redirect-utils';

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
