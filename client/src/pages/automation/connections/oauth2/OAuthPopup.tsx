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
    const {
        Component = (
            <div style={{margin: '12px'}} data-testid="popup-loading">
                Loading...
            </div>
        ),
    } = props;

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
                type: OAUTH_RESPONSE,
                error:
                    decodeURI(error) || 'OAuth error: An error has occurred.',
            });
        } else if (state && checkState(state)) {
            window.opener.postMessage({
                type: OAUTH_RESPONSE,
                payload,
            });
        } else {
            window.opener.postMessage({
                type: OAUTH_RESPONSE,
                error: 'OAuth error: State mismatch.',
            });
        }
    }, []);

    return Component;
};

export default OAuthPopup;
