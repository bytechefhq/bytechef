import useOAuth2 from '../oauth2/useOAuth2';
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

interface OAuth2ButtonProps {
    onClick: (getAuth: () => void) => void;
}

const OAuth2Button = ({onClick}: OAuth2ButtonProps) => {
    const {data, loading, error, getAuth} = useOAuth2({
        authorizeUrl: 'https://login.mailchimp.com/oauth2/authorize',
        clientId: '344111396868',
        redirectUri: `${document.location.origin}/callback`,
        scope: '',
        responseType: 'code',
        exchangeCodeForTokenServerURL: 'http://localhost:5173/token',
        exchangeCodeForTokenMethod: 'POST',
        onSuccess: (payload) => console.log('Success', payload),
        onError: (error_) => console.log('Error', error_),
    });

    const isLoggedIn = Boolean(data?.access_token);

    if (error) {
        return <div>Error</div>;
    }

    if (loading) {
        return (
            <Button
                icon={<LoadingIcon />}
                iconPosition="left"
                label="Creating..."
                type="button"
            />
        );
    }

    if (isLoggedIn) {
        return <pre>{JSON.stringify(data)}</pre>;
    }

    return (
        <Button
            label="Connect"
            onClick={() => onClick(getAuth)}
            type="submit"
        />
    );
};

export default OAuth2Button;
