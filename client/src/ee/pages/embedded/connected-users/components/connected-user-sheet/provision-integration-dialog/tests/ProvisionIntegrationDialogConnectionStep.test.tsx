import {AuthorizationType} from '@/ee/shared/middleware/embedded/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {FieldValues, useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProvisionIntegrationDialogConnectionStep from '../ProvisionIntegrationDialogConnectionStep';

const hoisted = vi.hoisted(() => ({
    useGetConnectionDefinitionQuery: vi.fn(),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: hoisted.useGetConnectionDefinitionQuery,
}));

const connectionDefinitionResult = {
    data: {
        authorizations: [
            {
                name: 'api_key',
                properties: [
                    {controlType: 'TEXT', label: 'Key', name: 'key', type: 'STRING'},
                    {controlType: 'TEXT', label: 'API key', name: 'value', type: 'STRING'},
                ],
                type: 'API_KEY',
            },
        ],
    },
};

const ConnectionStepHarness = ({
    authorizationType = AuthorizationType.ApiKey,
    oAuth2AuthorizationParameters,
}: {
    authorizationType?: AuthorizationType;
    oAuth2AuthorizationParameters?: {authorizationUrl?: string; clientId?: string};
}) => {
    const {control, formState} = useForm<FieldValues>({defaultValues: {connectionParameters: {}}});

    return (
        <ProvisionIntegrationDialogConnectionStep
            componentName="brevo"
            componentVersion={2}
            configuration={{
                integrationInstanceConfiguration: {authorizationType, name: 'Brevo Legacy'},
                oAuth2AuthorizationParameters,
                selfServe: false,
                sharedConnectionParameterNames: ['key'],
            }}
            control={control}
            formState={formState}
            onCodeSuccess={vi.fn()}
        />
    );
};

describe('ProvisionIntegrationDialogConnectionStep', () => {
    beforeEach(() => {
        hoisted.useGetConnectionDefinitionQuery.mockReset();
        hoisted.useGetConnectionDefinitionQuery.mockReturnValue(connectionDefinitionResult);
    });

    it('renders the connection fields with the real Properties component outside a workflow editor', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectionStepHarness />
            </QueryClientProvider>
        );

        expect(screen.getByText('API key')).toBeInTheDocument();
        expect(screen.queryByText('Key')).not.toBeInTheDocument();
    });

    it('renders the connection-level properties and hides the ones the configuration already provides', () => {
        hoisted.useGetConnectionDefinitionQuery.mockReturnValue({
            data: {
                ...connectionDefinitionResult.data,
                properties: [
                    {controlType: 'TEXT', label: 'Base URI', name: 'baseUri', type: 'STRING'},
                    {controlType: 'TEXT', label: 'Region', name: 'key', type: 'STRING'},
                ],
            },
        });

        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectionStepHarness />
            </QueryClientProvider>
        );

        expect(screen.getByText('Base URI')).toBeInTheDocument();
        expect(screen.getByText('API key')).toBeInTheDocument();
        expect(screen.queryByText('Region')).not.toBeInTheDocument();
    });

    it('renders a property once, as the authorization property, when both lists declare its name', () => {
        hoisted.useGetConnectionDefinitionQuery.mockReturnValue({
            data: {
                ...connectionDefinitionResult.data,
                properties: [{controlType: 'TEXT', label: 'Connection value', name: 'value', type: 'STRING'}],
            },
        });

        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectionStepHarness />
            </QueryClientProvider>
        );

        expect(screen.getAllByText('API key')).toHaveLength(1);
        expect(screen.queryByText('Connection value')).not.toBeInTheDocument();
    });

    it('requests the connection definition for the integration component version', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectionStepHarness />
            </QueryClientProvider>
        );

        expect(hoisted.useGetConnectionDefinitionQuery).toHaveBeenCalledWith({
            componentName: 'brevo',
            componentVersion: 2,
        });
    });

    it('explains an OAuth2 configuration whose app lacks the authorization URL or client ID', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectionStepHarness
                    authorizationType={AuthorizationType.Oauth2AuthorizationCode}
                    oAuth2AuthorizationParameters={{clientId: 'cid'}}
                />
            </QueryClientProvider>
        );

        expect(
            screen.getByText("This configuration's OAuth2 app is missing its authorization URL or client ID.")
        ).toBeInTheDocument();
    });

    it('does not show the OAuth2 misconfiguration message when the app is complete', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectionStepHarness
                    authorizationType={AuthorizationType.Oauth2AuthorizationCode}
                    oAuth2AuthorizationParameters={{authorizationUrl: 'https://auth.example.com', clientId: 'cid'}}
                />
            </QueryClientProvider>
        );

        expect(screen.queryByText(/missing its authorization URL/)).not.toBeInTheDocument();
    });
});
