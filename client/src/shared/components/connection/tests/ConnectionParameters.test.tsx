import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {AuthorizationType, ConnectionDefinition} from '@/shared/middleware/platform/configuration';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

const connectionDefinition = {
    authorizations: [
        {
            name: 'oauth2_authorization_code',
            properties: [{name: 'clientId'}, {name: 'clientSecret'}],
            title: 'OAuth2 Authorization Code',
            type: AuthorizationType.Oauth2AuthorizationCode,
        },
    ],
    properties: [],
} as unknown as ConnectionDefinition;

describe('ConnectionParameters', () => {
    it('shows entered authorization parameters under the Authorization Parameters heading', () => {
        render(
            <ConnectionParameters
                authorizationParameters={{clientId: 'my-client-id', clientSecret: 'my-client-secret'}}
                authorizationType={AuthorizationType.Oauth2AuthorizationCode}
                connectionDefinition={connectionDefinition}
                connectionParameters={{}}
            />
        );

        expect(screen.getByText('Authorization Parameters')).toBeInTheDocument();
        expect(screen.getByText('OAuth2 Authorization Code')).toBeInTheDocument();
        expect(screen.getByText('my-client-id')).toBeInTheDocument();
        expect(screen.getByText('my-client-secret')).toBeInTheDocument();
    });

    it('hides the heading when no authorization parameters were entered', () => {
        render(
            <ConnectionParameters
                authorizationParameters={{}}
                authorizationType={AuthorizationType.Oauth2AuthorizationCode}
                connectionDefinition={connectionDefinition}
                connectionParameters={{}}
            />
        );

        expect(screen.queryByText('Authorization Parameters')).not.toBeInTheDocument();
    });

    it('hides the heading when parameters are missing entirely', () => {
        render(<ConnectionParameters connectionDefinition={connectionDefinition} />);

        expect(screen.queryByText(/Parameters/)).not.toBeInTheDocument();
    });
});
