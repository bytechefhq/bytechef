import useOAuth2, {AuthTokenPayload} from '../oauth2/useOAuth2';
import Button from '../../../../components/Button/Button';
import React from 'react';

const LoadingIcon = (): JSX.Element => (
    <svg
        className="-ml-1 mr-1 h-4 w-4 animate-spin text-white"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
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
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
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
    redirectUri,
    responseType,
    scope,
    onError,
    onClick,
    onCodeSuccess,
    onTokenSuccess,
}: OAuth2ButtonProps) => {
    const {loading, getAuth} = useOAuth2({
        authorizationUrl: authorizationUrl,
        clientId: clientId,
        redirectUri: redirectUri,
        scope: scope,
        responseType: responseType,
        onCodeSuccess: onCodeSuccess,
        onTokenSuccess: onTokenSuccess,
        onError: onError,
    });

    if (loading) {
        return (
            <Button
                disabled={true}
                icon={<LoadingIcon />}
                iconPosition="left"
                label="Connecting..."
                type="button"
            />
        );
    }

    return (
        <Button
            label="Connect"
            type="submit"
            onClick={() => onClick(getAuth)}
        />
    );
};

export default OAuth2Button;
