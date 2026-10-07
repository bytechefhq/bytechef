import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {useAnalytics} from '@/shared/hooks/useAnalytics';

import useOAuth2, {CodePayloadI, OAuth2AbortReasonType, TokenPayloadI} from './oauth2/useOAuth2';

interface OAuth2ButtonProps {
    authorizationUrl: string;
    clientId: string;
    componentName?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    extraQueryParameters?: Record<string, any>;
    redirectUri: string;
    responseType: 'code' | 'token';
    scopes?: {[key: string]: boolean};
    onClick: (getAuth: () => void) => void;
    onCodeSuccess?: (payload: CodePayloadI) => void;
    onError?: (error: string) => void;
    onTokenSuccess?: (payload: TokenPayloadI) => void;
}

const OAuth2Button = ({
    authorizationUrl,
    clientId,
    componentName,
    extraQueryParameters,
    onClick,
    onCodeSuccess,
    onError,
    onTokenSuccess,
    redirectUri,
    responseType,
    scopes,
}: OAuth2ButtonProps) => {
    const {captureOAuth2AuthorizationFailed, captureOAuth2AuthorizationStarted, captureOAuth2AuthorizationSucceeded} =
        useAnalytics();

    const {cancel, getAuth, loading} = useOAuth2({
        authorizationUrl,
        clientId,
        extraQueryParameters,
        onAbort: (reason: OAuth2AbortReasonType) => {
            captureOAuth2AuthorizationFailed(componentName, reason);

            if (reason === 'timeout' && onError) {
                onError('The authorization window did not respond in time. Click Connect to try again.');
            }
        },
        onCodeSuccess: (payload: CodePayloadI) => {
            captureOAuth2AuthorizationSucceeded(componentName);

            if (onCodeSuccess) {
                onCodeSuccess(payload);
            }
        },
        onError: (error: string) => {
            captureOAuth2AuthorizationFailed(componentName, 'error');

            if (onError) {
                onError(error);
            }
        },
        onTokenSuccess: (payload: TokenPayloadI) => {
            captureOAuth2AuthorizationSucceeded(componentName);

            if (onTokenSuccess) {
                onTokenSuccess(payload);
            }
        },
        redirectUri,
        responseType,
        scopes,
    });

    return (
        <>
            {loading && <Button label="Cancel" onClick={cancel} type="button" variant="outline" />}

            <Button
                disabled={loading}
                icon={loading ? <LoadingIcon className="text-white" /> : undefined}
                label={loading ? 'Connecting...' : 'Connect'}
                onClick={() => {
                    if (!loading) {
                        captureOAuth2AuthorizationStarted(componentName);

                        onClick(getAuth);
                    }
                }}
                type={loading ? 'button' : 'submit'}
            />
        </>
    );
};

export default OAuth2Button;
