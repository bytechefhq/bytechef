import {MutableRefObject, useCallback, useRef, useState} from 'react';

import {OAUTH_BROADCAST_CHANNEL, OAUTH_RESPONSE, OAUTH_STATE_KEY, OAUTH_STORAGE_KEY} from './constants';
import {objectToQuery} from './tools';

export interface TokenPayloadI {
    token_type: string;
    expires_in: number;
    access_token: string;
    scope: string;
    refresh_token: string;
}

export interface CodePayloadI {
    code: string;
    [key: string]: string;
}

export interface Oauth2Props {
    authorizationUrl: string;
    clientId: string;
    redirectUri: string;
    responseType: 'code' | 'token';
    scope?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    extraQueryParameters?: Record<string, any>;
    onCodeSuccess?: (payload: CodePayloadI) => void;
    onError?: (error: string) => void;
    onTokenSuccess?: (payload: TokenPayloadI) => void;
}

const POPUP_HEIGHT = 800;
const POPUP_WIDTH = 600;

const enhanceAuthorizationUrl = (
    authorizeUrl: string,
    clientId: string,
    redirectUri: string,
    scope: string,
    state: string,
    responseType: Oauth2Props['responseType'],
    extraQueryParametersRef: MutableRefObject<Oauth2Props['extraQueryParameters']>
) => {
    const query = objectToQuery({
        client_id: clientId,
        redirect_uri: redirectUri,
        response_type: responseType,
        scope,
        state,
        ...extraQueryParametersRef.current,
    });

    return `${authorizeUrl}?${query}`;
};

// https://medium.com/@dazcyril/generating-cryptographic-random-state-in-javascript-in-the-browser-c538b3daae50
const generateState = () => {
    const validChars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let array = new Uint8Array(40);

    window.crypto.getRandomValues(array);

    array = array.map((x: number) => validChars.codePointAt(x % validChars.length)!);

    const stateObj = {
        origin: window.location.origin,
        random: String.fromCharCode.apply(null, Array.from(array)),
    };

    const json = JSON.stringify(stateObj);

    return btoa(encodeURIComponent(json));
};

const saveState = (state: string) => {
    sessionStorage.setItem(OAUTH_STATE_KEY, state);
};

const removeState = () => {
    sessionStorage.removeItem(OAUTH_STATE_KEY);
};

const openPopup = (url: string) => {
    // To fix issues with window.screen in multi-monitor setups, the easier option is to center the pop-up over the
    // parent window.
    const top = window.outerHeight / 2 + window.screenY - POPUP_HEIGHT / 2 - 50;
    const left = window.outerWidth / 2 + window.screenX - POPUP_WIDTH / 2;

    return window.open(url, 'OAuth2 Popup', `height=${POPUP_HEIGHT},width=${POPUP_WIDTH},top=${top},left=${left}`);
};

const closePopup = (popupRef: MutableRefObject<Window | null | undefined>) => {
    popupRef.current?.close();
};

const clearStorageResponse = () => {
    try {
        localStorage.removeItem(OAUTH_STORAGE_KEY);
    } catch {
        // Ignore localStorage errors
    }
};

const cleanup = (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    intervalRef: MutableRefObject<any>,
    popupRef: MutableRefObject<Window | null | undefined>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleMessageListener: any,
    broadcastChannel?: BroadcastChannel,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleStorageListener?: any
) => {
    clearInterval(intervalRef.current);
    closePopup(popupRef);
    removeState();
    clearStorageResponse();
    window.removeEventListener('message', handleMessageListener);

    if (broadcastChannel) {
        broadcastChannel.close();
    }

    if (handleStorageListener) {
        window.removeEventListener('storage', handleStorageListener);
    }
};

const useOAuth2 = (props: Oauth2Props) => {
    const {
        authorizationUrl,
        clientId,
        extraQueryParameters,
        onCodeSuccess,
        onError,
        onTokenSuccess,
        redirectUri,
        responseType,
        scope = '',
    } = props;

    const extraQueryParametersRef = useRef(extraQueryParameters);
    const popupRef = useRef<Window | null>();
    const curStateRef = useRef(undefined);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const intervalRef = useRef<any>();
    const [{error, loading}, setUI] = useState<{
        loading: boolean;
        error: string | null;
    }>({error: null, loading: false});

    const getAuth = useCallback(() => {
        // 1. Init
        setUI({
            error: null,
            loading: true,
        });

        // Clear any stale storage response from previous attempts
        clearStorageResponse();

        // 2. Generate and save state
        const state = generateState();
        saveState(state);

        // 3. Open popup
        popupRef.current = openPopup(
            enhanceAuthorizationUrl(
                authorizationUrl,
                clientId,
                redirectUri,
                scope,
                state,
                responseType,
                extraQueryParametersRef
            )
        );

        // Track listeners for cleanup
        let broadcastChannel: BroadcastChannel | undefined;

        // Shared function to process OAuth response data
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        function processOAuthResponse(data: any) {
            const type = data?.type;

            if (type !== OAUTH_RESPONSE || curStateRef.current === data?.payload?.state) {
                return;
            }

            // Validate state to prevent CSRF attacks
            const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);
            const receivedState = data?.payload?.state;

            if (!receivedState || savedState !== receivedState) {
                setUI({
                    error: 'OAuth error: State mismatch.',
                    loading: false,
                });

                if (onError) {
                    onError('OAuth error: State mismatch.');
                }

                doCleanup();

                return;
            }

            curStateRef.current = receivedState;

            try {
                const error = data?.error;

                if (error) {
                    setUI({
                        error: error || 'Unknown Error',
                        loading: false,
                    });

                    if (onError) {
                        onError(error);
                    }
                } else {
                    const payload = data?.payload;

                    if (responseType === 'code' && onCodeSuccess) {
                        onCodeSuccess(payload);
                    } else {
                        if (onTokenSuccess) {
                            onTokenSuccess(payload);
                        }
                    }

                    setUI({
                        error: null,
                        loading: false,
                    });
                }
            } catch (genericError) {
                console.error(genericError);

                setUI({
                    error: (genericError as Error).toString(),
                    loading: false,
                });
            } finally {
                doCleanup();
            }
        }

        // 4. Register message listener (primary method via window.opener.postMessage)
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        function handleMessageListener(message: MessageEvent<any>) {
            processOAuthResponse(message?.data);
        }
        window.addEventListener('message', handleMessageListener);

        // 5. Register BroadcastChannel listener (fallback when window.opener is lost)
        if (typeof BroadcastChannel !== 'undefined') {
            try {
                broadcastChannel = new BroadcastChannel(OAUTH_BROADCAST_CHANNEL);

                broadcastChannel.onmessage = (event) => {
                    processOAuthResponse(event.data);
                };
            } catch {
                // BroadcastChannel not supported in this context
            }
        }

        // 6. Register storage event listener (final fallback)
        const handleStorageListener = (event: StorageEvent) => {
            if (event.key !== OAUTH_STORAGE_KEY || !event.newValue) {
                return;
            }

            try {
                const data = JSON.parse(event.newValue);

                processOAuthResponse(data);
            } catch {
                // Ignore JSON parse errors
            }
        };
        window.addEventListener('storage', handleStorageListener);

        // Cleanup function that includes all listeners
        function doCleanup() {
            cleanup(intervalRef, popupRef, handleMessageListener, broadcastChannel, handleStorageListener);
        }

        // 7. Begin interval to check if popup was closed forcefully by the user
        // Also check localStorage directly in case storage event wasn't triggered (same-origin)
        intervalRef.current = setInterval(() => {
            const popupClosed = !popupRef.current?.window || popupRef.current?.window?.closed;

            if (popupClosed) {
                // Check localStorage for response (handles same-origin case where storage event doesn't fire)
                try {
                    const storedResponse = localStorage.getItem(OAUTH_STORAGE_KEY);

                    if (storedResponse) {
                        const data = JSON.parse(storedResponse);

                        processOAuthResponse(data);

                        return;
                    }
                } catch {
                    // Ignore localStorage errors
                }

                // Popup was closed before completing auth...
                setUI((currentUI) => ({
                    ...currentUI,
                    loading: false,
                }));

                console.warn('Warning: Popup was closed before completing authentication.');

                doCleanup();
            }
        }, 250);

        // 8. Remove listener(s) on unmount
        return () => {
            doCleanup();
        };
    }, [authorizationUrl, clientId, redirectUri, scope, responseType, onCodeSuccess, onTokenSuccess, onError, setUI]);

    return {error, getAuth, loading};
};

export default useOAuth2;
