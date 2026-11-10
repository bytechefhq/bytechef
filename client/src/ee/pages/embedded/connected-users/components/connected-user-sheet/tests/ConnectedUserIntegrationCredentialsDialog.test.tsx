import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactElement} from 'react';
import {useController} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserIntegrationCredentialsDialog from '../ConnectedUserIntegrationCredentialsDialog';

const hoisted = vi.hoisted(() => ({
    connectionDefinitionRequests: vi.fn(),
    connectionStepProps: vi.fn(),
    getIntegrationInstanceConnection: vi.fn(),
    getProvisionableIntegrations: vi.fn(),
    updateIntegrationInstanceCredentials: vi.fn(),
    useGetConnectionComponentDefinitionQuery: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    IntegrationInstanceApi: class {
        getIntegrationInstanceConnection = hoisted.getIntegrationInstanceConnection;
        getProvisionableIntegrations = hoisted.getProvisionableIntegrations;
        updateIntegrationInstanceCredentials = hoisted.updateIntegrationInstanceCredentials;
    },
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetConnectionComponentDefinitionQuery: hoisted.useGetConnectionComponentDefinitionQuery,
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: (request: unknown, enabled?: boolean) => {
        hoisted.connectionDefinitionRequests(request, enabled);

        return {
            data: {
                authorizations: [
                    {
                        properties: [
                            {controlType: 'TEXT', name: 'apiKey', type: 'STRING'},
                            {controlType: 'TEXT', name: 'clientId', type: 'STRING'},
                        ],
                        title: 'API Key',
                        type: 'API_KEY',
                    },
                    {
                        properties: [
                            {controlType: 'TEXT', name: 'clientId', type: 'STRING'},
                            {controlType: 'TEXT', name: 'clientSecret', type: 'STRING'},
                        ],
                        title: 'OAuth2 Authorization Code',
                        type: 'OAUTH2_AUTHORIZATION_CODE',
                    },
                ],
                properties: [],
            },
        };
    },
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialogConnectionStep',
    async (importOriginal) => {
        const actual =
            await importOriginal<
                typeof import('@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialogConnectionStep')
            >();

        const ActualConnectionStep = actual.default;

        return {
            ...actual,
            default: (props: Parameters<typeof ActualConnectionStep>[0]): ReactElement => {
                hoisted.connectionStepProps(props);

                return <ActualConnectionStep {...props} />;
            },
        };
    }
);

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => {
    const PropertyInput = ({control, name}: {control: never; name: string}) => {
        const {field} = useController({control, name});

        return <input aria-label={name.split('.').pop()} onChange={field.onChange} value={field.value ?? ''} />;
    };

    return {
        default: ({
            control,
            controlPath,
            properties,
        }: {
            control: never;
            controlPath: string;
            properties: {name: string}[];
        }) => (
            <div>
                {properties.map((property) => (
                    <PropertyInput control={control} key={property.name} name={`${controlPath}.${property.name}`} />
                ))}
            </div>
        ),
    };
});

vi.mock('@/shared/components/connection/OAuth2Button', () => ({
    default: ({onCodeSuccess}: {onCodeSuccess: (payload: {code: string; code_verifier?: string}) => void}) => (
        <button onClick={() => onCodeSuccess({code: 'auth-code', code_verifier: 'verifier'})} type="button">
            Authorize
        </button>
    ),
}));

const integrationInstance = {
    componentName: 'slack',
    enabled: true,
    id: 40,
    integrationId: 1,
    integrationInstanceConfigurationId: 2,
    integrationVersion: 1,
    provisioned: true,
};

const buildConfiguration = (id: number, authorizationType: string) => ({
    integrationInstanceConfiguration: {
        authorizationType,
        id,
        integrationId: 1,
        integrationVersion: 1,
        name: `Config ${id}`,
    },
    oAuth2AuthorizationParameters: {authorizationUrl: 'https://auth.example.com', clientId: 'cid'},
    redirectUri: 'https://app.example.com/callback',
    selfServe: false,
    sharedConnectionParameterNames: ['clientId', 'clientSecret'],
});

const mockProvisionableConfiguration = (configuration: ReturnType<typeof buildConfiguration> | undefined) => {
    hoisted.getProvisionableIntegrations.mockResolvedValue([
        {
            componentVersion: 1,
            integration: {componentName: 'slack', id: 1, name: 'Slack'},
            integrationInstanceConfigurations: configuration ? [configuration] : [],
        },
    ]);
};

const renderDialog = (onClose = vi.fn()) =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <ConnectedUserIntegrationCredentialsDialog
                connectedUserId={9}
                integrationInstance={integrationInstance}
                onClose={onClose}
            />
        </QueryClientProvider>
    );

describe('ConnectedUserIntegrationCredentialsDialog', () => {
    beforeEach(() => {
        hoisted.connectionDefinitionRequests.mockReset();
        hoisted.connectionStepProps.mockReset();
        hoisted.getIntegrationInstanceConnection.mockReset();
        hoisted.getProvisionableIntegrations.mockReset();
        hoisted.updateIntegrationInstanceCredentials.mockReset();
        hoisted.useGetConnectionComponentDefinitionQuery.mockReset();

        hoisted.useGetConnectionComponentDefinitionQuery.mockReturnValue({data: {name: 'slack', version: 3}});

        hoisted.updateIntegrationInstanceCredentials.mockResolvedValue(undefined);
        hoisted.getIntegrationInstanceConnection.mockResolvedValue({
            authorizationParameters: {apiKey: '••••••••'},
            authorizationType: 'API_KEY',
            componentName: 'slack',
            connectionVersion: 1,
            id: 70,
            name: 'Slack',
            parameters: {},
            version: 4,
        });

        mockProvisionableConfiguration(buildConfiguration(2, 'API_KEY'));
    });

    it('shows the provisioned connection parameters read-only', async () => {
        renderDialog();

        expect(await screen.findByText('Authorization Parameters')).toBeInTheDocument();
        expect(screen.getByText('••••••••')).toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        expect(hoisted.getIntegrationInstanceConnection).toHaveBeenCalledWith({id: 40});
    });

    it('resolves the connection definition through the connection version of the connection', async () => {
        renderDialog();

        expect(await screen.findByText('Authorization Parameters')).toBeInTheDocument();
        expect(hoisted.useGetConnectionComponentDefinitionQuery).toHaveBeenCalledWith(
            {componentName: 'slack', connectionVersion: 1},
            true
        );
        expect(hoisted.connectionDefinitionRequests).toHaveBeenCalledWith(
            {componentName: 'slack', componentVersion: 3},
            true
        );
        expect(hoisted.connectionDefinitionRequests).not.toHaveBeenCalledWith(
            {componentName: 'slack', componentVersion: 1},
            expect.anything()
        );
    });

    it('replaces the credentials with the entered values and the connection version', async () => {
        const onClose = vi.fn();

        renderDialog(onClose);

        await userEvent.click(await screen.findByRole('button', {name: 'Update credentials'}));

        expect(screen.queryByLabelText('clientId')).not.toBeInTheDocument();

        await userEvent.type(screen.getByLabelText('apiKey'), 'new-key');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateIntegrationInstanceCredentials).toHaveBeenCalledWith({
                id: 40,
                updateConnectionCredentialsRequest: {parameters: {apiKey: 'new-key'}, version: 4},
            })
        );
        await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    it('reloads the connection after a failed save so a retry sends the current version', async () => {
        const onClose = vi.fn();

        hoisted.updateIntegrationInstanceCredentials
            .mockRejectedValueOnce(new Error('Connection id=70 was modified by someone else'))
            .mockResolvedValueOnce(undefined);

        renderDialog(onClose);

        await userEvent.click(await screen.findByRole('button', {name: 'Update credentials'}));
        await userEvent.type(screen.getByLabelText('apiKey'), 'new-key');

        hoisted.getIntegrationInstanceConnection.mockResolvedValue({
            authorizationParameters: {apiKey: '••••••••'},
            authorizationType: 'API_KEY',
            componentName: 'slack',
            connectionVersion: 1,
            id: 70,
            name: 'Slack',
            parameters: {},
            version: 5,
        });

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.getIntegrationInstanceConnection).toHaveBeenCalledTimes(2));

        expect(onClose).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateIntegrationInstanceCredentials).toHaveBeenLastCalledWith({
                id: 40,
                updateConnectionCredentialsRequest: {parameters: {apiKey: 'new-key'}, version: 5},
            })
        );
        await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    it('is view-only when the integration configuration is no longer provisionable', async () => {
        mockProvisionableConfiguration(undefined);

        renderDialog();

        expect(
            await screen.findByText(
                "This integration's configuration can no longer be provisioned, so its credentials cannot be replaced."
            )
        ).toBeInTheDocument();
        expect(screen.getByText('Authorization Parameters')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Update credentials'})).not.toBeInTheDocument();
    });

    it("uses the configuration's OAuth2 app for OAuth2 integrations", async () => {
        const oAuth2Configuration = buildConfiguration(2, 'OAUTH2_AUTHORIZATION_CODE');

        mockProvisionableConfiguration(oAuth2Configuration);

        hoisted.getIntegrationInstanceConnection.mockResolvedValue({
            authorizationParameters: {clientId: 'cid', clientSecret: '••••••••'},
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
            componentName: 'slack',
            connectionVersion: 1,
            id: 70,
            name: 'Slack',
            parameters: {},
            version: 6,
        });

        renderDialog();

        await userEvent.click(await screen.findByRole('button', {name: 'Update credentials'}));

        expect(hoisted.connectionStepProps).toHaveBeenCalledWith(
            expect.objectContaining({
                componentName: 'slack',
                componentVersion: 1,
                configuration: oAuth2Configuration,
            })
        );
        expect(screen.queryByLabelText('clientSecret')).not.toBeInTheDocument();

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).toBeDisabled();

        await userEvent.click(screen.getByRole('button', {name: 'Authorize'}));

        await waitFor(() => expect(saveButton).toBeEnabled());

        await userEvent.click(saveButton);

        await waitFor(() =>
            expect(hoisted.updateIntegrationInstanceCredentials).toHaveBeenCalledWith({
                id: 40,
                updateConnectionCredentialsRequest: {
                    parameters: {code: 'auth-code', code_verifier: 'verifier'},
                    version: 6,
                },
            })
        );
    });
});
