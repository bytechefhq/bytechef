'use client';

import {useCallback, useEffect, useRef, useState} from 'react';

import useEmbedInit from '../../shared/useEmbedInit';
import useConnectDialog from '../connect-dialog';
import {MapObjectFieldsType} from '../connect-dialog/types';

const CONNECT_MESSAGE_TYPE = 'EMBED_OPEN_CONNECT_DIALOG';

const INTEGRATIONS_CHANGED_MESSAGE_TYPE = 'EMBED_INTEGRATIONS_CHANGED';

const ConnectDialogHost = ({
    baseUrl,
    environment,
    integrationId,
    jwtToken,
    mapObjectFields,
    mode,
    onClose,
}: {
    baseUrl: string;
    environment: string;
    integrationId: string;
    jwtToken: string;
    mapObjectFields?: MapObjectFieldsType;
    mode?: 'dark' | 'light';
    onClose: () => void;
}) => {
    const {closeDialog, openDialog} = useConnectDialog({
        baseUrl,
        environment,
        integrationId,
        jwtToken,
        mapObjectFields,
        mode,
        onClose,
    });

    useEffect(() => {
        openDialog();

        return () => {
            closeDialog();
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

interface IntegrationMarketplaceProps {
    baseUrl?: string;

    className?: string;

    environment?: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';

    jwtToken: string;

    mapObjectFields?: MapObjectFieldsType;

    onConnectDialogClose?: () => void;

    theme?: Record<string, unknown>;
}

const IntegrationMarketplace = ({
    baseUrl = 'https://app.bytechef.io',
    className,
    environment = 'PRODUCTION',
    jwtToken,
    mapObjectFields,
    onConnectDialogClose,
    theme,
}: IntegrationMarketplaceProps) => {
    const [connectingIntegrationId, setConnectingIntegrationId] = useState<string | null>(null);

    const iframeRef = useRef<HTMLIFrameElement>(null);

    useEmbedInit({baseUrl, iframeRef, params: {environment, jwtToken, theme}});

    const handleConnectDialogClose = useCallback(() => {
        setConnectingIntegrationId(null);

        iframeRef.current?.contentWindow?.postMessage(
            {type: INTEGRATIONS_CHANGED_MESSAGE_TYPE},
            new URL(baseUrl).origin
        );

        onConnectDialogClose?.();
    }, [baseUrl, onConnectDialogClose]);

    useEffect(() => {
        const targetOrigin = new URL(baseUrl).origin;

        const handleMessage = (event: MessageEvent) => {
            if (event.origin !== targetOrigin || event.source !== iframeRef.current?.contentWindow) {
                return;
            }

            if (event.data?.type === CONNECT_MESSAGE_TYPE && typeof event.data.integrationId === 'string') {
                setConnectingIntegrationId(event.data.integrationId);
            }
        };

        window.addEventListener('message', handleMessage);

        return () => {
            window.removeEventListener('message', handleMessage);
        };
    }, [baseUrl]);

    return (
        <div className={className}>
            <iframe
                height="100%"
                ref={iframeRef}
                src={`${baseUrl}/integration-marketplace.html#/embedded/marketplace`}
                style={{border: 'none'}}
                title="Integration Marketplace"
                width="100%"
            />

            {connectingIntegrationId && (
                <ConnectDialogHost
                    baseUrl={baseUrl}
                    environment={environment}
                    integrationId={connectingIntegrationId}
                    jwtToken={jwtToken}
                    key={connectingIntegrationId}
                    mapObjectFields={mapObjectFields}
                    mode={theme?.mode as 'dark' | 'light' | undefined}
                    onClose={handleConnectDialogClose}
                />
            )}
        </div>
    );
};

export default IntegrationMarketplace;
