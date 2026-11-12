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

vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: ({environmentId}: {environmentId: number}) => <span>{`Environment ${environmentId}`}</span>,
}));

vi.mock('@/shared/components/connection/OAuth2Button', () => ({
    default: ({
        onClick,
        onCodeSuccess,
    }: {
        onClick: (getAuth: () => void) => void;
        onCodeSuccess: (payload: {code: string; code_verifier?: string}) => void;
    }) => (
        <button
            onClick={() => onClick(() => onCodeSuccess({code: 'auth-code', code_verifier: 'verifier'}))}
            type="button"
        >
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

const renderDialog = (onClose = vi.fn(), queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}})) =>
    render(
        <QueryClientProvider client={queryClient}>
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
            environmentId: 2,
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

    it('is laid out like the Edit Connection dialog', async () => {
        renderDialog();

        expect(await screen.findByRole('heading', {name: 'Edit Connection'})).toBeInTheDocument();
        expect(await screen.findByText('Environment 2')).toBeInTheDocument();
        expect(screen.getByText('Environment')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
    });

    it('shows the credential fields above the read-only parameters without a heading of their own', async () => {
        renderDialog();

        await userEvent.click(await screen.findByRole('button', {name: 'Update credentials'}));

        const apiKeyInput = screen.getByLabelText('apiKey');
        const authorizationParametersHeading = screen.getByText('Authorization Parameters');

        expect(
            apiKeyInput.compareDocumentPosition(authorizationParametersHeading) & Node.DOCUMENT_POSITION_FOLLOWING
        ).toBeTruthy();
        expect(screen.queryByText('Connection')).not.toBeInTheDocument();
    });

    it('goes back to the read-only view without saving', async () => {
        renderDialog();

        await userEvent.click(await screen.findByRole('button', {name: 'Update credentials'}));
        await userEvent.click(screen.getByRole('button', {name: 'Back'}));

        expect(screen.queryByLabelText('apiKey')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Update credentials'})).toBeInTheDocument();
        expect(hoisted.updateIntegrationInstanceCredentials).not.toHaveBeenCalled();
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
        await userEvent.click(screen.getByRole('button', {name: 'Update credentials'}));

        await waitFor(() =>
            expect(hoisted.updateIntegrationInstanceCredentials).toHaveBeenCalledWith({
                id: 40,
                updateConnectionCredentialsRequest: {parameters: {apiKey: 'new-key'}, version: 4},
            })
        );
        await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    it("refreshes the connected user's connections after the credentials are replaced", async () => {
        const onClose = vi.fn();
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderDialog(onClose, queryClient);

        await userEvent.click(await screen.findByRole('button', {name: 'Update credentials'}));
        await userEvent.type(screen.getByLabelText('apiKey'), 'new-key');
        await userEvent.click(screen.getByRole('button', {name: 'Update credentials'}));

        await waitFor(() => expect(onClose).toHaveBeenCalled());

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['embedded_connections']});
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

        await userEvent.click(screen.getByRole('button', {name: 'Update credentials'}));

        await waitFor(() => expect(hoisted.getIntegrationInstanceConnection).toHaveBeenCalledTimes(2));

        expect(onClose).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Update credentials'}));

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

        expect(screen.queryByRole('button', {name: 'Update credentials'})).not.toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Authorize'})).toHaveLength(1);

        await userEvent.click(screen.getByRole('button', {name: 'Authorize'}));

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

    it('offers Update credentials to retry an OAuth2 authorization whose save failed', async () => {
        const oAuth2Configuration = buildConfiguration(2, 'OAUTH2_AUTHORIZATION_CODE');

        mockProvisionableConfiguration(oAuth2Configuration);

        hoisted.updateIntegrationInstanceCredentials
            .mockRejectedValueOnce(new Error('Connection id=70 was modified by someone else'))
            .mockResolvedValueOnce(undefined);

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
        await userEvent.click(screen.getByRole('button', {name: 'Authorize'}));

        const saveButton = await screen.findByRole('button', {name: 'Update credentials'});

        expect(screen.queryByRole('button', {name: 'Authorize'})).not.toBeInTheDocument();

        await waitFor(() => expect(saveButton).toBeEnabled());

        await userEvent.click(saveButton);

        await waitFor(() => expect(hoisted.updateIntegrationInstanceCredentials).toHaveBeenCalledTimes(2));

        expect(hoisted.updateIntegrationInstanceCredentials).toHaveBeenLastCalledWith({
            id: 40,
            updateConnectionCredentialsRequest: {
                parameters: {code: 'auth-code', code_verifier: 'verifier'},
                version: 6,
            },
        });
    });
});
