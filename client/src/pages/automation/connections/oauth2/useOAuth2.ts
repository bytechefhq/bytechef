import {useCallback, useRef, useState} from 'react';
import useLocalStorageState from 'use-local-storage-state';
import {OAUTH_RESPONSE, OAUTH_STATE_KEY} from './constants';
import {objectToQuery, queryToObject} from './tools';

export type AuthTokenPayload = {
    token_type: string;
    expires_in: number;
    access_token: string;
    scope: string;
    refresh_token: string;
};

export type ResponseTypeBasedProps<TData> =
    | {
          responseType: 'code';
          exchangeCodeForTokenServerURL: string;
          exchangeCodeForTokenMethod?: 'POST' | 'GET';
          onSuccess?: (payload: TData) => void; // TODO as this payload will be custom
          // TODO Adjust payload type
      }
    | {
          responseType: 'token';
          onSuccess?: (payload: TData) => void; // TODO Adjust payload type
      };

export type Oauth2Props<TData = AuthTokenPayload> = {
    authorizeUrl: string;
    clientId: string;
    redirectUri: string;
    scope?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    extraQueryParameters?: Record<string, any>;
    onError?: (error: string) => void;
} & ResponseTypeBasedProps<TData>;

export type State<TData = AuthTokenPayload> = TData | null;

const POPUP_HEIGHT = 800;
const POPUP_WIDTH = 600;
const DEFAULT_EXCHANGE_CODE_FOR_TOKEN_METHOD = 'POST';

const enhanceAuthorizeUrl = (
    authorizeUrl: string,
    clientId: string,
    redirectUri: string,
    scope: string,
    state: string,
    responseType: Oauth2Props['responseType'],
    extraQueryParametersRef: React.MutableRefObject<
        Oauth2Props['extraQueryParameters']
    >
) => {
    const query = objectToQuery({
        response_type: responseType,
        client_id: clientId,
        redirect_uri: redirectUri,
        scope,
        state,
        ...extraQueryParametersRef.current,
    });

    return `${authorizeUrl}?${query}`;
};

// https://medium.com/@dazcyril/generating-cryptographic-random-state-in-javascript-in-the-browser-c538b3daae50
const generateState = () => {
    const validChars =
        'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let array = new Uint8Array(40);

    window.crypto.getRandomValues(array);

    array = array.map(
        (x: number) => validChars.codePointAt(x % validChars.length)!
    );

    return String.fromCharCode.apply(null, Array.from(array));
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
    const left = window.outerWidth / 2 + window.screenX - POPUP_WIDTH / 2 - 400;

    return window.open(
        url,
        'OAuth2 Popup',
        `height=${POPUP_HEIGHT},width=${POPUP_WIDTH},top=${top},left=${left}`
    );
};

const closePopup = (
    popupRef: React.MutableRefObject<Window | null | undefined>
) => {
    popupRef.current?.close();
};

const cleanup = (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    intervalRef: React.MutableRefObject<any>,
    popupRef: React.MutableRefObject<Window | null | undefined>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleMessageListener: any
) => {
    clearInterval(intervalRef.current);
    closePopup(popupRef);
    removeState();
    window.removeEventListener('message', handleMessageListener);
};

const formatExchangeCodeForTokenServerURL = (
    exchangeCodeForTokenServerURL: string,
    clientId: string,
    code: string,
    redirectUri: string,
    state: string
) => {
    const url = exchangeCodeForTokenServerURL.split('?')[0];
    const anySearchParameters = queryToObject(
        exchangeCodeForTokenServerURL.split('?')[1]
    );

    return `${url}?${objectToQuery({
        ...anySearchParameters,
        client_id: clientId,
        grant_type: 'authorization_code',
        code,
        redirect_uri: redirectUri,
        state,
    })}`;
};

const useOAuth2 = <TData = AuthTokenPayload>(props: Oauth2Props<TData>) => {
    const {
        authorizeUrl,
        clientId,
        redirectUri,
        scope = '',
        responseType,
        extraQueryParameters = {},
        onSuccess,
        onError,
    } = props;

    const extraQueryParametersRef = useRef(extraQueryParameters);
    const popupRef = useRef<Window | null>();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const intervalRef = useRef<any>();
    const [{loading, error}, setUI] = useState<{
        loading: boolean;
        error: string | null;
    }>({loading: false, error: null});
    const [data, setData] = useLocalStorageState<State>(
        `${responseType}-${authorizeUrl}-${clientId}-${scope}`,
        {
            defaultValue: null,
        }
    );

    const exchangeCodeForTokenServerURL =
        responseType === 'code' && props.exchangeCodeForTokenServerURL;
    const exchangeCodeForTokenMethod =
        responseType === 'code' && props.exchangeCodeForTokenMethod;

    const getAuth = useCallback(() => {
        // 1. Init
        setUI({
            loading: true,
            error: null,
        });

        // 2. Generate and save state
        const state = generateState();
        saveState(state);

        // 3. Open popup
        popupRef.current = openPopup(
            enhanceAuthorizeUrl(
                authorizeUrl,
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
            if (type !== OAUTH_RESPONSE) {
                return;
            }

            try {
                const errorMaybe = message?.data?.error;

                if (errorMaybe) {
                    setUI({
                        loading: false,
                        error: errorMaybe || 'Unknown Error',
                    });

                    if (onError) {
                        await onError(errorMaybe);
                    }
                } else {
                    let payload = message?.data?.payload;

                    if (
                        responseType === 'code' &&
                        exchangeCodeForTokenServerURL
                    ) {
                        const response = await fetch(
                            formatExchangeCodeForTokenServerURL(
                                exchangeCodeForTokenServerURL,
                                clientId,
                                payload?.code,
                                redirectUri,
                                state
                            ),
                            {
                                method:
                                    exchangeCodeForTokenMethod ||
                                    DEFAULT_EXCHANGE_CODE_FOR_TOKEN_METHOD,
                            }
                        );

                        payload = await response.json();
                    }

                    setUI({
                        loading: false,
                        error: null,
                    });
                    setData(payload);

                    if (onSuccess) {
                        await onSuccess(payload);
                    }
                }
            } catch (genericError) {
                console.error(genericError);

                setUI({
                    loading: false,
                    error: (genericError as Error).toString(),
                });
            } finally {
                // Clear stuff ...
                cleanup(intervalRef, popupRef, handleMessageListener);
            }
        }
        window.addEventListener('message', handleMessageListener);

        // 4. Begin interval to check if popup was closed forcefully by the user
        intervalRef.current = setInterval(() => {
            const popupClosed =
                !popupRef.current?.window || popupRef.current?.window?.closed;
            if (popupClosed) {
                // Popup was closed before completing auth...
                setUI((ui) => ({
                    ...ui,
                    loading: false,
                }));

                console.warn(
                    'Warning: Popup was closed before completing authentication.'
                );

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
    }, [
        authorizeUrl,
        clientId,
        redirectUri,
        scope,
        responseType,
        exchangeCodeForTokenServerURL,
        exchangeCodeForTokenMethod,
        onSuccess,
        onError,
        setUI,
        setData,
    ]);

    return {data, loading, error, getAuth};
};

export default useOAuth2;
