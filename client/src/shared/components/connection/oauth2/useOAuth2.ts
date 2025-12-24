import {MutableRefObject, useCallback, useRef, useState} from 'react';

import {OAUTH_RESPONSE, OAUTH_STATE_KEY} from './constants';
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

const cleanup = (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    intervalRef: MutableRefObject<any>,
    popupRef: MutableRefObject<Window | null | undefined>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleMessageListener: any
) => {
    clearInterval(intervalRef.current);
    closePopup(popupRef);
    removeState();
    window.removeEventListener('message', handleMessageListener);
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

        // 4. Register message listener
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        async function handleMessageListener(message: MessageEvent<any>) {
            const type = message?.data?.type;

            if (type !== OAUTH_RESPONSE || curStateRef.current === message?.data?.payload?.state) {
                return;
            }

            curStateRef.current = message?.data?.payload.state;

            try {
                const error = message?.data?.error;

                if (error) {
                    setUI({
                        error: error || 'Unknown Error',
                        loading: false,
                    });

                    if (onError) {
                        await onError(error);
                    }
                } else {
                    const payload = message?.data?.payload;

                    if (responseType === 'code' && onCodeSuccess) {
                        await onCodeSuccess(payload);
                    } else {
                        if (onTokenSuccess) {
                            await onTokenSuccess(payload);
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
                // Clear stuff ...
                cleanup(intervalRef, popupRef, handleMessageListener);
            }
        }
        window.addEventListener('message', handleMessageListener);

        // 4. Begin interval to check if popup was closed forcefully by the user
        intervalRef.current = setInterval(() => {
            const popupClosed = !popupRef.current?.window || popupRef.current?.window?.closed;
            if (popupClosed) {
                // Popup was closed before completing auth...
                setUI((ui) => ({
                    ...ui,
                    loading: false,
                }));

                console.warn('Warning: Popup was closed before completing authentication.');

                clearInterval(intervalRef.current);
                removeState();
                window.removeEventListener('message', handleMessageListener);
            }
        }, 250);

        // 5. Remove listener(s) on unmount
        return () => {
            window.removeEventListener('message', handleMessageListener);

            if (intervalRef.current) {
                clearInterval(intervalRef.current);
            }
        };
    }, [authorizationUrl, clientId, redirectUri, scope, responseType, onCodeSuccess, onTokenSuccess, onError, setUI]);

    return {error, getAuth, loading};
};

export default useOAuth2;
