import {useConnectDialog} from '@bytechef/embedded-react';
import {useEffect, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';

import './connect.css';

const ENVIRONMENTS = ['DEVELOPMENT', 'STAGING', 'PRODUCTION'] as const;

interface JwtPayloadI {
    env: number;
    exp: number;
    integrationId: number;
}

interface TokenDataI {
    environment: string;
    integrationId: number;
    jwtToken: string;
}

type ParseResultType =
    | {status: 'error'; reason: 'corrupted'}
    | {status: 'error'; reason: 'expired'}
    | {status: 'success'; tokenData: TokenDataI};

function decodeJwtPayload(jwtToken: string): JwtPayloadI | null {
    try {
        const payloadBase64 = jwtToken.split('.')[1];

        const payloadJson = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'));

        return JSON.parse(payloadJson) as JwtPayloadI;
    } catch (error) {
        console.error('Failed to decode JWT payload:', error);

        return null;
    }
}

function parseTokenData(jwtToken: string): ParseResultType {
    const payload = decodeJwtPayload(jwtToken);

    if (!payload) {
        return {reason: 'corrupted', status: 'error'};
    }

    if (payload.exp * 1000 < Date.now()) {
        return {reason: 'expired', status: 'error'};
    }

    const environment = ENVIRONMENTS[payload.env];

    if (!environment) {
        return {reason: 'corrupted', status: 'error'};
    }

    return {
        status: 'success',
        tokenData: {
            environment,
            integrationId: payload.integrationId,
            jwtToken,
        },
    };
}

function ConnectApp({tokenData}: {tokenData: TokenDataI}) {
    const openDialogRef = useRef<(() => void) | null>(null);

    const dialog = useConnectDialog({
        baseUrl: window.location.origin,
        environment: tokenData.environment,
        integrationId: String(tokenData.integrationId),
        jwtToken: tokenData.jwtToken,
    });

    openDialogRef.current = dialog.openDialog;

    useEffect(() => {
        openDialogRef.current?.();
    }, []);

    return null;
}

type ConnectPageStateType = 'error' | 'expired' | 'loading' | 'ready';

function ConnectPage() {
    const [pageState, setPageState] = useState<ConnectPageStateType>('loading');
    const [tokenData, setTokenData] = useState<TokenDataI | null>(null);

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const jwtToken = urlParams.get('token');

        if (!jwtToken) {
            setPageState('expired');

            return;
        }

        const parseResult = parseTokenData(jwtToken);

        if (parseResult.status === 'error') {
            setPageState(parseResult.reason === 'expired' ? 'expired' : 'error');

            return;
        }

        setTokenData(parseResult.tokenData);
        setPageState('ready');
    }, []);

    return (
        <div className="connect-container">
            <div className="connect-card">
                <div className="connect-logo">
                    <h1>ByteChef</h1>
                </div>

                {pageState === 'loading' && (
                    <div className="connect-expired">
                        <p>Loading...</p>
                    </div>
                )}

                {pageState === 'expired' && (
                    <div className="connect-expired">
                        <div className="connect-expired-icon">&#9888;</div>

                        <h2>Link Expired</h2>

                        <p>This connection link has expired. Please request a new link from your MCP client.</p>
                    </div>
                )}

                {pageState === 'error' && (
                    <div className="connect-expired">
                        <div className="connect-expired-icon">&#9888;</div>

                        <h2>Invalid Link</h2>

                        <p>
                            This connection link is invalid or corrupted. Please request a new link from your MCP
                            client.
                        </p>
                    </div>
                )}

                {pageState === 'ready' && tokenData && <ConnectApp tokenData={tokenData} />}
            </div>
        </div>
    );
}

const rootElement = document.getElementById('connect-root');

if (rootElement) {
    createRoot(rootElement).render(<ConnectPage />);
}
