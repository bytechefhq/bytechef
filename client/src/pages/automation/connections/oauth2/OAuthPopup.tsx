import {useEffect} from 'react';

import {OAUTH_RESPONSE, OAUTH_STATE_KEY} from './constants';
import {queryToObject} from './tools';

const checkState = (receivedState: string) => {
    const state = sessionStorage.getItem(OAUTH_STATE_KEY);
    return state === receivedState;
};

type Props = {
    Component?: React.ReactElement;
};

const OAuthPopup = (props: Props) => {
    const {Component = <div className="flex h-screen items-center justify-center text-xl">Loading...</div>} = props;

    // On mount
    useEffect(() => {
        const payload = {
            ...queryToObject(window.location.search.split('?')[1]),
            ...queryToObject(window.location.hash.split('#')[1]),
        };
        const state = payload?.state;
        const error = payload?.error;

        if (!window.opener) {
            throw new Error('No window opener');
        }

        if (error) {
            window.opener.postMessage({
                error: decodeURI(error) || 'OAuth error: An error has occurred.',
                type: OAUTH_RESPONSE,
            });
        } else if (state && checkState(state)) {
            window.opener.postMessage({
                payload,
                type: OAUTH_RESPONSE,
            });
        } else {
            window.opener.postMessage({
                error: 'OAuth error: State mismatch.',
                type: OAUTH_RESPONSE,
            });
        }
    }, []);

    return Component;
};

export default OAuthPopup;
