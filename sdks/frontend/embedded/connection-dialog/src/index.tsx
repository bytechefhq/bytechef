import Dialog from './Dialog';
import React, {useState} from 'react';
import './styles.css';

const JWT =
    'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNIVmliR2xqT20xaGQwOUdOSFYyUVVsd1RXdHFlakZ1T1U5YVZGZ3dLMHhJV1hoUWJ6WlcifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.NDL-cUc9i7DCwVmtRVEos4pUmGPZjx0BSEpMQkqhEXTg0bA-I0nPBoZfej3z6tSdFK5qPFVu3lW42crkKqo-lxo07U4AsbDxRuPWR4ELQdHAA0e3GDRFI-fnSJ4FkNR39-IGWyHiSxuOkyYVS8TyQ2BrSB5D48yF_ooTY-Qvs-r9zJ-mI7ksqtF3V_-KeqHNvQRJY8A4WhmrmS1XTGeTbdLCWdNr7TeMa-AFNszEHyIk4_th-H2iHosyFLqAiD7L1RdpkzIXDlsMlLkI2dTjezJaTwwpCJDcInlZSB-wlK0X9Sat1c1JY9eECLpIK1vL6N3MZMhia4yNj0kI_9vhKg';

// Define the return type explicitly to help TypeScript understand the structure
interface ConnectionDialogHook {
    openDialog: () => void;
    closeDialog: () => void;
    DialogComponent: React.FC;
}

export default function useEmbeddedByteChefConnectionDialog({
    baseUrl = 'http://localhost:9555',
    integrationId = '1050',
}: {
    baseUrl?: string;
    integrationId?: string;
}): ConnectionDialogHook {
    const [isOpen, setIsOpen] = useState(false);

    async function getData() {
        const url = `${baseUrl}/api/embedded/v1/integrations/${integrationId}`;

        console.log('url', url);

        try {
            const response = await fetch(url, {
                headers: {
                    Authorization: `Bearer ${JWT}`,
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                    'X-Environment': 'production',
                },
            });

            if (!response.ok) {
                throw new Error(`Response status: ${response.status}`);
            }

            const json = await response.json();

            console.log(json);
        } catch (error: any) {
            console.error(error?.message);
        }
    }


    const openDialog = async () => {
        console.log('openDialog called');
        getData();

        setIsOpen(true);
    };

    const closeDialog = () => {
        setIsOpen(false);
    };

    const handleConnect = () => {
        closeDialog();
    };

    const DialogComponent: React.FC = () => (
        <Dialog closeDialog={closeDialog} handleConnect={handleConnect} isOpen={isOpen} />
    );

    return {
        openDialog,
        closeDialog,
        DialogComponent,
    };
}
