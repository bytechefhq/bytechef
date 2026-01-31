import {render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import OAuthPopup from '../OAuthPopup';
import {OAUTH_RESPONSE, OAUTH_STORAGE_KEY} from '../constants';

// Mock BroadcastChannel
class MockBroadcastChannel {
    static lastInstance: MockBroadcastChannel | null = null;
    static postMessageCalls: unknown[] = [];
    name: string;

    constructor(name: string) {
        this.name = name;
        MockBroadcastChannel.lastInstance = this;
    }

    postMessage(data: unknown) {
        MockBroadcastChannel.postMessageCalls.push(data);
    }

    close() {
        // No-op
    }

    static reset() {
        MockBroadcastChannel.lastInstance = null;
        MockBroadcastChannel.postMessageCalls = [];
    }
}

describe('OAuthPopup', () => {
    const originalLocation = window.location;
    let mockPostMessage: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        MockBroadcastChannel.reset();
        vi.stubGlobal('BroadcastChannel', MockBroadcastChannel);

        // Mock window.opener
        mockPostMessage = vi.fn();
        Object.defineProperty(window, 'opener', {
            configurable: true,
            value: {
                postMessage: mockPostMessage,
            },
            writable: true,
        });

        // Clear localStorage
        localStorage.clear();
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
        MockBroadcastChannel.reset();
        Object.defineProperty(window, 'location', {
            configurable: true,
            value: originalLocation,
            writable: true,
        });
    });

    function setWindowLocation(search: string, hash: string) {
        Object.defineProperty(window, 'location', {
            configurable: true,
            value: {
                ...originalLocation,
                hash,
                search,
            },
            writable: true,
        });
    }

    it('should send success message via window.opener.postMessage', () => {
        setWindowLocation('?code=auth-code-123&state=test-state', '');

        render(<OAuthPopup />);

        expect(mockPostMessage).toHaveBeenCalledWith(
            {
                payload: {
                    code: 'auth-code-123',
                    state: 'test-state',
                },
                type: OAUTH_RESPONSE,
            },
            '*'
        );

        expect(screen.getByText('Authentication successful! You can close this window.')).toBeInTheDocument();
    });

    it('should send error message when OAuth returns error', () => {
        setWindowLocation('?error=access_denied&state=test-state', '');

        render(<OAuthPopup />);

        expect(mockPostMessage).toHaveBeenCalledWith(
            {
                error: 'access_denied',
                type: OAUTH_RESPONSE,
            },
            '*'
        );

        expect(screen.getByText('access_denied')).toBeInTheDocument();
    });

    it('should handle hash fragment parameters (implicit flow)', () => {
        setWindowLocation('', '#access_token=token-123&token_type=Bearer&state=test-state');

        render(<OAuthPopup />);

        expect(mockPostMessage).toHaveBeenCalledWith(
            {
                payload: {
                    access_token: 'token-123',
                    state: 'test-state',
                    token_type: 'Bearer',
                },
                type: OAUTH_RESPONSE,
            },
            '*'
        );
    });

    it('should combine query and hash parameters', () => {
        setWindowLocation('?code=auth-code', '#state=test-state&extra=value');

        render(<OAuthPopup />);

        expect(mockPostMessage).toHaveBeenCalledWith(
            {
                payload: {
                    code: 'auth-code',
                    extra: 'value',
                    state: 'test-state',
                },
                type: OAUTH_RESPONSE,
            },
            '*'
        );
    });

    it('should use BroadcastChannel as fallback when window.opener.postMessage fails', () => {
        mockPostMessage.mockImplementation(() => {
            throw new Error('postMessage failed');
        });

        setWindowLocation('?code=auth-code-123&state=test-state', '');

        render(<OAuthPopup />);

        // Should have tried postMessage first
        expect(mockPostMessage).toHaveBeenCalled();

        // Should have used BroadcastChannel as fallback
        expect(MockBroadcastChannel.postMessageCalls).toContainEqual({
            payload: {
                code: 'auth-code-123',
                state: 'test-state',
            },
            type: OAUTH_RESPONSE,
        });
    });

    it('should use BroadcastChannel when window.opener is null', () => {
        Object.defineProperty(window, 'opener', {
            configurable: true,
            value: null,
            writable: true,
        });

        setWindowLocation('?code=auth-code-123&state=test-state', '');

        render(<OAuthPopup />);

        // Should have used BroadcastChannel
        expect(MockBroadcastChannel.postMessageCalls).toContainEqual({
            payload: {
                code: 'auth-code-123',
                state: 'test-state',
            },
            type: OAUTH_RESPONSE,
        });
    });

    it('should store message in localStorage as final fallback', () => {
        Object.defineProperty(window, 'opener', {
            configurable: true,
            value: null,
            writable: true,
        });

        setWindowLocation('?code=auth-code-123&state=test-state', '');

        render(<OAuthPopup />);

        const storedData = localStorage.getItem(OAUTH_STORAGE_KEY);

        expect(storedData).toBeTruthy();

        const parsed = JSON.parse(storedData!);

        expect(parsed.payload).toEqual({
            code: 'auth-code-123',
            state: 'test-state',
        });
        expect(parsed.type).toBe(OAUTH_RESPONSE);
        expect(parsed.timestamp).toBeDefined();
    });

    it('should show error message when all communication methods fail', () => {
        // Remove window.opener
        Object.defineProperty(window, 'opener', {
            configurable: true,
            value: null,
            writable: true,
        });

        // Make BroadcastChannel fail
        vi.stubGlobal('BroadcastChannel', function () {
            throw new Error('BroadcastChannel not supported');
        });

        // Make localStorage fail
        vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
            throw new Error('localStorage not available');
        });

        setWindowLocation('?code=auth-code-123&state=test-state', '');

        render(<OAuthPopup />);

        expect(
            screen.getByText('Error: Could not communicate with parent window. Please close this window and try again.')
        ).toBeInTheDocument();
    });

    it('should decode URI-encoded error messages', () => {
        setWindowLocation('?error=access%20denied%20by%20user&state=test-state', '');

        render(<OAuthPopup />);

        expect(mockPostMessage).toHaveBeenCalledWith(
            {
                error: 'access denied by user',
                type: OAUTH_RESPONSE,
            },
            '*'
        );

        expect(screen.getByText('access denied by user')).toBeInTheDocument();
    });

    it('should render custom component when provided', () => {
        setWindowLocation('?code=auth-code-123&state=test-state', '');

        render(<OAuthPopup Component={<div data-testid="custom-component">Custom Loading...</div>} />);

        expect(screen.getByTestId('custom-component')).toBeInTheDocument();
        expect(screen.getByText('Custom Loading...')).toBeInTheDocument();
    });

    it('should use all three communication methods simultaneously for reliability', () => {
        setWindowLocation('?code=auth-code-123&state=test-state', '');

        render(<OAuthPopup />);

        // All three methods should be attempted
        expect(mockPostMessage).toHaveBeenCalled();
        expect(MockBroadcastChannel.postMessageCalls.length).toBeGreaterThan(0);
        expect(localStorage.getItem(OAUTH_STORAGE_KEY)).toBeTruthy();
    });

    it('should treat empty error as success (falsy check)', () => {
        setWindowLocation('?error=&state=test-state', '');

        render(<OAuthPopup />);

        // Empty string is falsy, so it's treated as success and sends payload
        expect(mockPostMessage).toHaveBeenCalledWith(
            {
                payload: {
                    error: '',
                    state: 'test-state',
                },
                type: OAUTH_RESPONSE,
            },
            '*'
        );
    });
});
