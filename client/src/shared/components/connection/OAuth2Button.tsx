import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';

import useOAuth2, {CodePayloadI, TokenPayloadI} from './oauth2/useOAuth2';

interface OAuth2ButtonProps {
    authorizationUrl: string;
    clientId: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    extraQueryParameters?: Record<string, any>;
    redirectUri: string;
    responseType: 'code' | 'token';
    scope?: string;
    onClick: (getAuth: () => void) => void;
    onCodeSuccess?: (payload: CodePayloadI) => void;
    onError?: (error: string) => void;
    onTokenSuccess?: (payload: TokenPayloadI) => void;
}

const OAuth2Button = ({
    authorizationUrl,
    clientId,
    extraQueryParameters,
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
        extraQueryParameters,
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
            icon={loading ? <LoadingIcon className="text-white" /> : undefined}
            label={loading ? 'Connecting...' : 'Connect'}
            onClick={() => {
                if (!loading) {
                    onClick(getAuth);
                }
            }}
            type={loading ? 'button' : 'submit'}
        />
    );
};

export default OAuth2Button;
