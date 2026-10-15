declare module '@bytechef/embedded' {
    interface UseConnectDialogProps {
        baseUrl?: string;
        connectionOnly?: boolean;
        environment?: string;
        integrationId: string;
        integrationInstanceId?: string;
        jwtToken: string;
        mode?: 'dark' | 'light';
        onClose?: () => void;
        onConnected?: (connectionId: number) => void;
        onReauthorized?: (connectionId: number) => void;
        reauthorizeConnectionId?: number;
    }

    interface ConnectionDialogHookReturnI {
        closeDialog: () => void;
        openDialog: () => void;
    }

    interface ActivationDialogProps {
        baseUrl?: string;
        connectionCreationAllowed?: boolean;
        environment?: string;
        jwtToken: string;
        mode?: 'dark' | 'light';
        onActivated?: (workflowUuid: string) => void;
        onClose: () => void;
        onEditWorkflow?: (workflowUuid: string) => void;
        workflowTemplateId: string;
    }

    export function ActivationDialog(props: ActivationDialogProps): JSX.Element | null;

    export function useConnectDialog(props: UseConnectDialogProps): ConnectionDialogHookReturnI;
}
