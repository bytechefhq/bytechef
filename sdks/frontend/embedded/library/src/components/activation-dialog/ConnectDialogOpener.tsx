import {useEffect, useRef} from 'react';
import useConnectDialog from '../connect-dialog/index';

interface ConnectDialogOpenerProps {
    baseUrl: string;
    environment: string;
    integrationId: number;
    jwtToken: string;
    mode?: 'dark' | 'light';
    onConnected: (connectionId: number | undefined) => void;
    onDone: () => void;
}

/**
 * Mounts `useConnectDialog` in connection-only mode and opens it immediately. One opener exists at a time because
 * the connect dialog renders into a single shared portal container.
 */
const ConnectDialogOpener = ({
    baseUrl,
    environment,
    integrationId,
    jwtToken,
    mode,
    onConnected,
    onDone,
}: ConnectDialogOpenerProps) => {
    const openedRef = useRef(false);

    const {openDialog} = useConnectDialog({
        baseUrl,
        connectionOnly: true,
        environment,
        integrationId: String(integrationId),
        jwtToken,
        mode,
        onClose: onDone,
        onConnected: (_integrationInstanceId, connectionId) => onConnected(connectionId),
    });

    useEffect(() => {
        if (openedRef.current) {
            return;
        }

        openedRef.current = true;

        void openDialog();
        // The opener's whole life is one open; the hook removes its portal on unmount.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

export default ConnectDialogOpener;
