import {Button} from '@/components/ui/button';

import useOAuth2, {AuthTokenPayload} from '../oauth2/useOAuth2';

const LoadingIcon = () => (
    <svg
        className="-ml-1 mr-1 h-4 w-4 animate-spin text-white"
        fill="none"
        viewBox="0 0 24 24"
        xmlns="http://www.w3.org/2000/svg"
    >
        <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
        />

        <path
            className="opacity-75"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            fill="currentColor"
        />
    </svg>
);

type OAuth2ButtonProps = {
    authorizationUrl: string;
    clientId: string;
    redirectUri: string;
    responseType: 'code' | 'token';
    scope?: string;
    onClick: (getAuth: () => void) => void;
    onCodeSuccess?: (code: string) => void;
    onError?: (error: string) => void;
    onTokenSuccess?: (payload: AuthTokenPayload) => void;
};

const OAuth2Button = ({
    authorizationUrl,
    clientId,
    onClick,
    onCodeSuccess,
    onError,
    onTokenSuccess,
    redirectUri,
    responseType,
    scope,
}: OAuth2ButtonProps) => {
    const {getAuth, loading} = useOAuth2({
        authorizationUrl,
        clientId,
        onCodeSuccess,
        onError,
        onTokenSuccess,
        redirectUri,
        responseType,
        scope,
    });

    return (
        <Button
            disabled={loading}
            onClick={() => {
                if (!loading) {
                    onClick(getAuth);
                }
            }}
            type={loading ? 'button' : 'submit'}
        >
            {loading && <LoadingIcon />} {loading ? 'Connecting...' : 'Connect'}
        </Button>
    );
};

export default OAuth2Button;
