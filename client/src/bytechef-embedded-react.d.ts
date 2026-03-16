declare module '@bytechef/embedded-react' {
    interface UseConnectDialogProps {
        baseUrl?: string;
        environment?: string;
        integrationId: string;
        integrationInstanceId?: string;
        jwtToken: string;
    }

    interface ConnectionDialogHookReturnI {
        closeDialog: () => void;
        openDialog: () => void;
    }

    export function useConnectDialog(props: UseConnectDialogProps): ConnectionDialogHookReturnI;
}
