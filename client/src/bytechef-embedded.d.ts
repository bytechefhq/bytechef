declare module '@bytechef/embedded' {
    interface UseConnectDialogBaseProps {
        baseUrl?: string;
        environment?: string;
        integrationId: string;
        integrationInstanceId?: string;
        jwtToken: string;
        mode?: 'dark' | 'light';
        onClose?: () => void;
    }

    interface FullModePropsI {
        connectionOnly?: false;
        onConnected?: never;
        onReauthorized?: never;
        reauthorizeConnectionId?: never;
    }

    interface ConnectionOnlyModePropsI {
        connectionOnly: true;
        onConnected?: (integrationInstanceId: number, connectionId: number | undefined) => void;
        onReauthorized?: never;
        reauthorizeConnectionId?: never;
    }

    interface ReauthorizeModePropsI {
        connectionOnly?: false;
        onConnected?: never;
        onReauthorized?: (connectionId: number) => void;
        reauthorizeConnectionId: number;
    }

    type UseConnectDialogPropsType = UseConnectDialogBaseProps &
        (FullModePropsI | ConnectionOnlyModePropsI | ReauthorizeModePropsI);

    interface DialogHookReturnI {
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

    type UseActivationDialogPropsType = Omit<ActivationDialogProps, 'onClose'> & {onClose?: () => void};

    export function ActivationDialog(props: ActivationDialogProps): import('react').JSX.Element;

    export function useActivationDialog(props: UseActivationDialogPropsType): DialogHookReturnI;

    export function useConnectDialog(props: UseConnectDialogPropsType): DialogHookReturnI;
}
