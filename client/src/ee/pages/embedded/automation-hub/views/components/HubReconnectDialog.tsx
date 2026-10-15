import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {getEmbedCredentials} from '@/ee/pages/embedded/shared/embedCredentials';
import {useConnectDialog} from '@bytechef/embedded';
import {useEffect} from 'react';

interface HubReconnectDialogProps {
    connectionId: number;
    integrationId: number;
    onDone: () => void;
    onReauthorized: () => void;
}

/**
 * Mounts the SDK connect dialog in reauthorize mode and opens it at once. The view mounts one of these at a time:
 * the SDK dialog renders into a single shared portal container, so a second instance would never appear.
 */
const HubReconnectDialog = ({connectionId, integrationId, onDone, onReauthorized}: HubReconnectDialogProps) => {
    const themeMode = useAutomationHubStore((state) => state.theme.mode);

    const {environment, jwtToken} = getEmbedCredentials();

    const {closeDialog, openDialog} = useConnectDialog({
        baseUrl: import.meta.env.VITE_API_BASE_PATH || '',
        environment: environment || 'PRODUCTION',
        integrationId: String(integrationId),
        jwtToken: jwtToken || '',
        mode: themeMode,
        onClose: onDone,
        onReauthorized,
        reauthorizeConnectionId: connectionId,
    });

    useEffect(() => {
        openDialog();

        return () => closeDialog();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

export default HubReconnectDialog;
