import {ReactElement, useEffect, useState} from 'react';

import {OAUTH_BROADCAST_CHANNEL, OAUTH_RESPONSE, OAUTH_STORAGE_KEY} from './constants';
import {queryToObject} from './tools';

interface OAuthMessageI {
    error?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    payload?: Record<string, any>;
    timestamp?: number;
    type: string;
}

// Send message using available methods (postMessage, BroadcastChannel, localStorage)
function sendOAuthMessage(message: OAuthMessageI): boolean {
    let messageSent = false;

    // Method 1: Try window.opener.postMessage (preferred)
    if (window.opener) {
        try {
            window.opener.postMessage(message, '*');
            messageSent = true;
        } catch (error) {
            console.warn('Failed to send message via window.opener:', error);
        }
    }

    // Method 2: Try BroadcastChannel API (fallback for lost opener)
    if (typeof BroadcastChannel !== 'undefined') {
        try {
            const channel = new BroadcastChannel(OAUTH_BROADCAST_CHANNEL);

            channel.postMessage(message);
            channel.close();
            messageSent = true;
        } catch (error) {
            console.warn('Failed to send message via BroadcastChannel:', error);
        }
    }

    // Method 3: Use localStorage as final fallback
    try {
        localStorage.setItem(
            OAUTH_STORAGE_KEY,
            JSON.stringify({
                ...message,
                timestamp: Date.now(),
            })
        );
        messageSent = true;
    } catch (error) {
        console.warn('Failed to send message via localStorage:', error);
    }

    return messageSent;
}

interface OAuthPopupProps {
    Component?: ReactElement;
}

const OAuthPopup = (props: OAuthPopupProps) => {
    const [message, setMessage] = useState('Loading...');

    const {Component = <div className="flex h-screen items-center justify-center text-xl">{message}</div>} = props;

    useEffect(() => {
        const payload = {
            ...queryToObject(window.location.search.split('?')[1]),
            ...queryToObject(window.location.hash.split('#')[1]),
        };
        const error = payload?.error;

        if (error) {
            const errorMessage = decodeURI(error) || 'OAuth error: An error has occurred.';

            setMessage(errorMessage);

            const sent = sendOAuthMessage({
                error: errorMessage,
                type: OAUTH_RESPONSE,
            });

            if (!sent) {
                setMessage('Error: Could not communicate with parent window. Please close this window and try again.');
            }
        } else {
            setMessage('Authentication successful! You can close this window.');

            const sent = sendOAuthMessage({
                payload,
                type: OAUTH_RESPONSE,
            });

            if (!sent) {
                setMessage('Error: Could not communicate with parent window. Please close this window and try again.');
            }
        }
    }, []);

    return Component;
};

export default OAuthPopup;
