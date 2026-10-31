import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useOAuth2 from './useOAuth2';

const OAUTH_RESPONSE = 'react-use-oauth2-response';
const OAUTH_STATE_KEY = 'react-use-oauth2-state-key';

const pkceQueryParameters = {
    code_challenge: 'test-challenge',
    code_challenge_method: 'S256',
    code_verifier: 'test-verifier',
};

describe('useOAuth2', () => {
    const originalOpen = window.open;
    const stopAuthFunctions: (() => void)[] = [];

    beforeEach(() => {
        window.open = vi.fn().mockReturnValue({close: vi.fn(), window: {closed: false}} as unknown as Window);

        sessionStorage.clear();
    });

    afterEach(() => {
        for (const stopAuth of stopAuthFunctions) {
            stopAuth();
        }

        stopAuthFunctions.length = 0;

        vi.restoreAllMocks();

        window.open = originalOpen;

        sessionStorage.clear();
    });

    it('keeps the PKCE code verifier out of the authorization URL', () => {
        const {result} = renderHook(() =>
            useOAuth2({
                authorizationUrl: 'https://auth.example.com/authorize',
                clientId: 'test-client-id',
                extraQueryParameters: pkceQueryParameters,
                redirectUri: 'https://app.example.com/oauth',
                responseType: 'code',
            })
        );

        act(() => {
            stopAuthFunctions.push(result.current.getAuth());
        });

        const url = new URL(vi.mocked(window.open).mock.calls[0][0] as string);

        expect(url.searchParams.get('code_challenge')).toBe('test-challenge');
        expect(url.searchParams.get('code_challenge_method')).toBe('S256');
        expect(url.searchParams.has('code_verifier')).toBe(false);
    });

    it('passes the PKCE code verifier with the authorization code', () => {
        const onCodeSuccess = vi.fn();

        const {result} = renderHook(() =>
            useOAuth2({
                authorizationUrl: 'https://auth.example.com/authorize',
                clientId: 'test-client-id',
                extraQueryParameters: pkceQueryParameters,
                onCodeSuccess,
                redirectUri: 'https://app.example.com/oauth',
                responseType: 'code',
            })
        );

        act(() => {
            stopAuthFunctions.push(result.current.getAuth());
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {payload: {code: 'auth-code-123', state: savedState}, type: OAUTH_RESPONSE},
                })
            );
        });

        expect(onCodeSuccess).toHaveBeenCalledWith({
            code: 'auth-code-123',
            code_verifier: 'test-verifier',
            state: savedState,
        });
    });
});
