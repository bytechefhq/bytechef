import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {OAUTH_RESPONSE, OAUTH_STATE_KEY, OAUTH_STORAGE_KEY} from '../constants';
import useOAuth2, {Oauth2Props} from '../useOAuth2';

// Mock BroadcastChannel
class MockBroadcastChannel {
    static instances: MockBroadcastChannel[] = [];
    name: string;
    onmessage: ((event: MessageEvent) => void) | null = null;

    constructor(name: string) {
        this.name = name;
        MockBroadcastChannel.instances.push(this);
    }

    postMessage(data: unknown) {
        // Simulate broadcasting to all other instances with the same name
        MockBroadcastChannel.instances
            .filter((instance) => instance !== this && instance.name === this.name)
            .forEach((instance) => {
                if (instance.onmessage) {
                    instance.onmessage(new MessageEvent('message', {data}));
                }
            });
    }

    close() {
        const index = MockBroadcastChannel.instances.indexOf(this);

        if (index > -1) {
            MockBroadcastChannel.instances.splice(index, 1);
        }
    }

    static reset() {
        MockBroadcastChannel.instances = [];
    }
}

describe('useOAuth2', () => {
    let mockWindow: Window | null = null;
    const originalOpen = window.open;
    const originalAddEventListener = window.addEventListener;
    const registeredListeners: Array<{type: string; listener: EventListener}> = [];

    const defaultProps: Oauth2Props = {
        authorizationUrl: 'https://auth.example.com/authorize',
        clientId: 'test-client-id',
        redirectUri: 'https://app.example.com/oauth',
        responseType: 'code',
        scope: 'read write',
    };

    beforeEach(() => {
        // Reset mocks completely
        vi.clearAllMocks();
        MockBroadcastChannel.reset();
        vi.stubGlobal('BroadcastChannel', MockBroadcastChannel);

        // Track and wrap addEventListener to enable cleanup
        registeredListeners.length = 0;
        window.addEventListener = vi.fn((type: string, listener: EventListener) => {
            registeredListeners.push({listener, type});
            originalAddEventListener.call(window, type, listener);
        }) as typeof window.addEventListener;

        // Mock window.open
        mockWindow = {
            close: vi.fn(),
            closed: false,
            window: {closed: false},
        } as unknown as Window;

        window.open = vi.fn().mockReturnValue(mockWindow);

        // Clear storage
        sessionStorage.clear();
        localStorage.clear();
    });

    afterEach(() => {
        // Remove all registered event listeners to prevent test pollution
        for (const {listener, type} of registeredListeners) {
            window.removeEventListener(type, listener);
        }

        registeredListeners.length = 0;

        vi.restoreAllMocks();
        vi.unstubAllGlobals();
        window.addEventListener = originalAddEventListener;
        window.open = originalOpen;
        MockBroadcastChannel.reset();

        // Clear any remaining storage
        sessionStorage.clear();
        localStorage.clear();
    });

    it('should initialize with loading false and no error', () => {
        const {result} = renderHook(() => useOAuth2(defaultProps));

        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBeNull();
        expect(typeof result.current.getAuth).toBe('function');
    });

    it('should set loading to true when getAuth is called', () => {
        const {result} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        expect(result.current.loading).toBe(true);
    });

    it('should open popup with correct URL parameters', () => {
        const {result} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        expect(window.open).toHaveBeenCalledTimes(1);

        const openCall = vi.mocked(window.open).mock.calls[0];
        const url = new URL(openCall[0] as string);

        expect(url.origin + url.pathname).toBe('https://auth.example.com/authorize');
        expect(url.searchParams.get('client_id')).toBe('test-client-id');
        expect(url.searchParams.get('redirect_uri')).toBe('https://app.example.com/oauth');
        expect(url.searchParams.get('response_type')).toBe('code');
        expect(url.searchParams.get('scope')).toBe('read write');
        expect(url.searchParams.get('state')).toBeTruthy();
    });

    it('should save state to sessionStorage', () => {
        const {result, unmount} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        expect(savedState).toBeTruthy();

        // Cleanup to prevent interference with subsequent tests
        unmount();
    });

    it('should handle successful code response via postMessage', () => {
        const onCodeSuccess = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onCodeSuccess,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        // Simulate OAuth callback via postMessage
        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {
                        payload: {
                            code: 'auth-code-123',
                            state: savedState,
                        },
                        type: OAUTH_RESPONSE,
                    },
                })
            );
        });

        expect(onCodeSuccess).toHaveBeenCalledWith({
            code: 'auth-code-123',
            state: savedState,
        });
        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBeNull();
    });

    it('should handle successful token response via postMessage', () => {
        const onTokenSuccess = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onTokenSuccess,
            responseType: 'token',
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        // Simulate OAuth callback via postMessage
        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {
                        payload: {
                            access_token: 'token-123',
                            expires_in: 3600,
                            refresh_token: 'refresh-123',
                            scope: 'read write',
                            state: savedState,
                            token_type: 'Bearer',
                        },
                        type: OAUTH_RESPONSE,
                    },
                })
            );
        });

        expect(onTokenSuccess).toHaveBeenCalled();
        expect(result.current.loading).toBe(false);
    });

    it('should handle OAuth error via postMessage', () => {
        const onError = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onError,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        // Simulate OAuth error via postMessage
        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {
                        error: 'access_denied',
                        payload: {state: savedState},
                        type: OAUTH_RESPONSE,
                    },
                })
            );
        });

        expect(onError).toHaveBeenCalledWith('access_denied');
        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBe('access_denied');
    });

    it('should handle state mismatch error', () => {
        const onError = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onError,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        // Simulate OAuth callback with wrong state
        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {
                        payload: {
                            code: 'auth-code-123',
                            state: 'wrong-state',
                        },
                        type: OAUTH_RESPONSE,
                    },
                })
            );
        });

        expect(onError).toHaveBeenCalledWith('OAuth error: State mismatch.');
        expect(result.current.error).toBe('OAuth error: State mismatch.');
    });

    it('should handle successful response via BroadcastChannel fallback', () => {
        const onCodeSuccess = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onCodeSuccess,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        // Simulate OAuth callback via BroadcastChannel (fallback when window.opener is lost)
        // Create another channel with the same name and post a message
        act(() => {
            const senderChannel = new MockBroadcastChannel('react-use-oauth2-broadcast-channel');

            senderChannel.postMessage({
                payload: {
                    code: 'auth-code-456',
                    state: savedState,
                },
                type: OAUTH_RESPONSE,
            });
            senderChannel.close();
        });

        expect(onCodeSuccess).toHaveBeenCalledWith({
            code: 'auth-code-456',
            state: savedState,
        });
        expect(result.current.loading).toBe(false);
    });

    it('should handle storage event for cross-tab communication', () => {
        const onCodeSuccess = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onCodeSuccess,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        const storageData = JSON.stringify({
            payload: {
                code: 'auth-code-storage',
                state: savedState,
            },
            timestamp: Date.now(),
            type: OAUTH_RESPONSE,
        });

        // Simulate storage event (cross-tab communication)
        act(() => {
            const storageEvent = new StorageEvent('storage', {
                key: OAUTH_STORAGE_KEY,
                newValue: storageData,
            });

            window.dispatchEvent(storageEvent);
        });

        expect(onCodeSuccess).toHaveBeenCalledWith({
            code: 'auth-code-storage',
            state: savedState,
        });
        expect(result.current.loading).toBe(false);
    });

    it('should ignore storage events for other keys', () => {
        const onCodeSuccess = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onCodeSuccess,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        // Simulate storage event for a different key
        act(() => {
            const storageEvent = new StorageEvent('storage', {
                key: 'some-other-key',
                newValue: JSON.stringify({
                    payload: {
                        code: 'should-be-ignored',
                        state: savedState,
                    },
                    type: OAUTH_RESPONSE,
                }),
            });

            window.dispatchEvent(storageEvent);
        });

        expect(onCodeSuccess).not.toHaveBeenCalled();
        expect(result.current.loading).toBe(true);
    });

    it('should clean up localStorage response on getAuth', () => {
        // Pre-populate localStorage with stale data
        localStorage.setItem(OAUTH_STORAGE_KEY, 'stale-data');

        const {result} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        // Stale data should be cleared
        expect(localStorage.getItem(OAUTH_STORAGE_KEY)).toBeNull();
    });

    it('should ignore messages with different type', () => {
        const onCodeSuccess = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onCodeSuccess,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        // Simulate message with wrong type
        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {
                        payload: {
                            code: 'should-be-ignored',
                            state: savedState,
                        },
                        type: 'wrong-type',
                    },
                })
            );
        });

        expect(onCodeSuccess).not.toHaveBeenCalled();
        expect(result.current.loading).toBe(true);
    });

    it('should include extra query parameters in authorization URL', () => {
        const props: Oauth2Props = {
            ...defaultProps,
            extraQueryParameters: {
                audience: 'https://api.example.com',
                prompt: 'consent',
            },
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const openCall = vi.mocked(window.open).mock.calls[0];
        const url = new URL(openCall[0] as string);

        expect(url.searchParams.get('audience')).toBe('https://api.example.com');
        expect(url.searchParams.get('prompt')).toBe('consent');
    });

    it('should register BroadcastChannel listener on getAuth', () => {
        renderHook(() => useOAuth2(defaultProps));

        // Before getAuth, no BroadcastChannel instances
        expect(MockBroadcastChannel.instances.length).toBe(0);

        const {result} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        // After getAuth, should have a BroadcastChannel instance
        expect(MockBroadcastChannel.instances.length).toBeGreaterThan(0);
        expect(MockBroadcastChannel.instances[0].name).toBe('react-use-oauth2-broadcast-channel');
    });

    it('should register storage event listener on getAuth', () => {
        const addEventListenerSpy = vi.spyOn(window, 'addEventListener');

        const {result} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        expect(addEventListenerSpy).toHaveBeenCalledWith('storage', expect.any(Function));
    });

    it('should register message event listener on getAuth', () => {
        const addEventListenerSpy = vi.spyOn(window, 'addEventListener');

        const {result} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        expect(addEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
    });

    it('should handle popup closing with localStorage data', () => {
        vi.useFakeTimers();

        const onCodeSuccess = vi.fn();
        const props: Oauth2Props = {
            ...defaultProps,
            onCodeSuccess,
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        // Simulate localStorage being populated by the OAuth callback page
        localStorage.setItem(
            OAUTH_STORAGE_KEY,
            JSON.stringify({
                payload: {
                    code: 'auth-code-from-storage',
                    state: savedState,
                },
                timestamp: Date.now(),
                type: OAUTH_RESPONSE,
            })
        );

        // Simulate popup closing
        (mockWindow as Window & {window: {closed: boolean}}).window.closed = true;

        // Advance timers to trigger the interval check
        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(onCodeSuccess).toHaveBeenCalledWith({
            code: 'auth-code-from-storage',
            state: savedState,
        });
        expect(result.current.loading).toBe(false);

        vi.useRealTimers();
    });

    it('should set loading to false when popup closes without auth data', () => {
        vi.useFakeTimers();

        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        const {result} = renderHook(() => useOAuth2(defaultProps));

        act(() => {
            result.current.getAuth();
        });

        // Simulate popup closing without any OAuth data
        (mockWindow as Window & {window: {closed: boolean}}).window.closed = true;

        // Advance timers to trigger the interval check
        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(result.current.loading).toBe(false);
        expect(consoleWarnSpy).toHaveBeenCalledWith('Warning: Popup was closed before completing authentication.');

        consoleWarnSpy.mockRestore();
        vi.useRealTimers();
    });
});
