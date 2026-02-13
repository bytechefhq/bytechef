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

const POPUP_HEIGHT = 800;
const POPUP_WIDTH = 600;

const POPUP_CLOSED_GRACE_PERIOD_MS = 120_000;

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

type ResponseType = 'code' | 'token';

export interface UseOAuth2Props {
    authorizationUrl: string;
    clientId: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    extraQueryParameters?: Record<string, any>;
    onCodeSuccess?: (payload: CodePayloadI) => void;
    onError?: (error: string) => void;
    onTokenSuccess?: (payload: TokenPayloadI) => void;
    redirectUri: string;
    responseType: ResponseType;
    scopes?: {[key: string]: boolean};
}

const useOAuth2 = ({
    authorizationUrl,
    clientId,
    extraQueryParameters,
    onCodeSuccess,
    onError,
    onTokenSuccess,
    redirectUri,
    responseType,
    scopes,
}: UseOAuth2Props) => {
    const [{error, loading}, setUI] = useState<{
        loading: boolean;
        error: string | null;
    }>({error: null, loading: false});

    const extraQueryParametersRef = useRef(extraQueryParameters);
    const popupRef = useRef<Window | null>();
    const curStateRef = useRef(undefined);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const intervalRef = useRef<any>();

    const getAuth = useCallback(() => {
        setUI({
            error: null,
            loading: true,
        });

        clearStorageResponse();

        const state = generateState();

        saveState(state);

        const selectedScopeKeys = Object.entries(scopes ?? {})
            .filter(([, selected]) => selected)
            .map(([key]) => key);

        const selectedScopes = selectedScopeKeys.join(' ');

        const authQuery = objectToQuery({
            client_id: clientId,
            redirect_uri: redirectUri,
            response_type: responseType,
            scope: selectedScopes,
            state,
            ...extraQueryParametersRef.current,
        });

        const popupUrl = `${authorizationUrl}?${authQuery}`;

        popupRef.current = openPopup(popupUrl);

        let broadcastChannel: BroadcastChannel | undefined;

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

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        function handleMessageListener(message: MessageEvent<any>) {
            processOAuthResponse(message?.data);
        }
        window.addEventListener('message', handleMessageListener);

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

        function doCleanup() {
            cleanup(intervalRef, popupRef, handleMessageListener, broadcastChannel, handleStorageListener);
        }

        let popupClosedAt: number | null = null;

        intervalRef.current = setInterval(() => {
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

            let popupAppearsClosed = false;

            try {
                popupAppearsClosed = !popupRef.current || popupRef.current.closed;
            } catch {
                popupAppearsClosed = false;
            }

            if (popupAppearsClosed) {
                if (!popupClosedAt) {
                    popupClosedAt = Date.now();
                }

                if (Date.now() - popupClosedAt > POPUP_CLOSED_GRACE_PERIOD_MS) {
                    setUI((currentUI) => ({
                        ...currentUI,
                        loading: false,
                    }));

                    console.warn('Warning: Popup was closed before completing authentication.');

                    doCleanup();
                }
            } else {
                popupClosedAt = null;
            }
        }, 250);

        return () => {
            doCleanup();
        };
    }, [scopes, clientId, redirectUri, responseType, authorizationUrl, onError, onCodeSuccess, onTokenSuccess]);

    return {error, getAuth, loading};
};

export default useOAuth2;
