import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialog from '../IntegrationInstanceConfigurationDialog';

const hoisted = vi.hoisted(() => ({
    connectionDefinition: undefined as {authorizations: {title: string; type: string}[]} | undefined,
    createMutate: vi.fn(),
    grantedScopes: undefined as string[] | undefined,
    integration: undefined as {componentName: string} | undefined,
    updateMutate: vi.fn(),
    workflows: [] as {id: string; workflowUuid: string}[],
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', () => ({
    UNION_ENVIRONMENT_ID: -1,
    useEmbeddedScopeState: () => ({editionUnknown: false, error: false, granted: true, loading: false}),
    useHasEmbeddedScope: (scope: string) =>
        hoisted.grantedScopes === undefined || hoisted.grantedScopes.includes(scope),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useCreateIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
    useUpdateIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    IntegrationInstanceConfigurationKeys: {integrationInstanceConfigurations: ['integrationInstanceConfigurations']},
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries', () => ({
    IntegrationInstanceConfigurationTagKeys: {integrationInstanceConfigurationTags: ['tags']},
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: hoisted.workflows, isPending: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {integrations: ['integrations']},
    useGetIntegrationQuery: () => ({data: hoisted.integration}),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: () => ({data: hoisted.connectionDefinition}),
}));

vi.mock('@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore', () => ({
    useWorkflowsEnabledStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({reset: vi.fn(), setWorkflowEnabled: vi.fn(), workflowEnabledMap: new Map()}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 1}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationInstanceConfigurationCreated: vi.fn()}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../IntegrationInstanceConfigurationDialogBasicStep', () => ({
    default: ({
        authorizations,
        setCurIntegrationId,
        setCurIntegrationVersion,
        setValue,
    }: {
        authorizations?: {type: string}[];
        setCurIntegrationId: (integrationId: number) => void;
        setCurIntegrationVersion: (integrationVersion: number) => void;
        setValue: (name: string, value: unknown) => void;
    }) => (
        <div data-testid="basic-step">
            <span data-testid="basic-step-authorizations">
                {authorizations?.map((authorization) => authorization.type).join(',')}
            </span>

            <button onClick={() => setValue('authorizationType', 'API_KEY')} type="button">
                Use API Key
            </button>

            <button
                onClick={() => {
                    setCurIntegrationId(2);
                    setCurIntegrationVersion(1);
                }}
                type="button"
            >
                Pick Integration
            </button>
        </div>
    ),
}));

vi.mock('../IntegrationInstanceConfigurationDialogOauth2Step', () => ({
    default: () => <div data-testid="oauth2-step" />,
}));

vi.mock('../IntegrationInstanceConfigurationDialogWorkflowsStep', () => ({
    default: () => <div data-testid="workflows-step" />,
}));

vi.mock('@/shared/components/connection/ConnectionParameters', () => ({
    default: ({credentialsHidden}: {credentialsHidden?: boolean}) => (
        <div data-credentials-hidden={String(!!credentialsHidden)} data-testid="connection-parameters" />
    ),
}));

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    act(() => {
        authenticationStore.setState({account: undefined, authenticated: false});
    });

    resetAll();
    vi.clearAllMocks();

    hoisted.connectionDefinition = undefined;
    hoisted.grantedScopes = undefined;
    hoisted.integration = undefined;
    hoisted.workflows = [];
});

describe('IntegrationInstanceConfigurationDialog', () => {
    describe('create mode', () => {
        it('should title the dialog for a new configuration and name the current step', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('heading', {name: /New Instance Configuration/})).toBeInTheDocument();
            expect(screen.getByRole('heading', {name: /Basic/})).toBeInTheDocument();
        });

        it('should render the basic step', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByTestId('basic-step')).toBeInTheDocument();
        });
    });

    describe('create scope', () => {
        it('should offer Save for a new configuration only with the create scope', async () => {
            hoisted.connectionDefinition = {authorizations: [{title: 'API Key', type: 'API_KEY'}]};
            hoisted.integration = {componentName: 'httpClient'};
            hoisted.grantedScopes = ['INTEGRATION_INSTANCE_CONFIGURATION_EDIT'];

            const withoutCreateRender = render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={{integrationId: 2, integrationVersion: 1, name: 'New'}}
                    onClose={onClose}
                />
            );

            expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();

            withoutCreateRender.unmount();

            hoisted.grantedScopes = ['INTEGRATION_INSTANCE_CONFIGURATION_CREATE'];

            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={{integrationId: 2, integrationVersion: 1, name: 'New'}}
                    onClose={onClose}
                />
            );

            expect(await screen.findByRole('button', {name: 'Save'})).toBeInTheDocument();
        });
    });

    describe('authorization', () => {
        beforeEach(() => {
            hoisted.connectionDefinition = {
                authorizations: [
                    {title: 'API Key', type: 'API_KEY'},
                    {title: 'OAuth2', type: 'OAUTH2_AUTHORIZATION_CODE'},
                ],
            };
            hoisted.integration = {componentName: 'httpClient'};
        });

        it('should pass the connection authorizations to the basic step', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByTestId('basic-step-authorizations')).toHaveTextContent(
                'API_KEY,OAUTH2_AUTHORIZATION_CODE'
            );
        });

        it('should default to the OAuth2 authorization and offer the OAuth2 step', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            expect(await screen.findByTestId('oauth2-step')).toBeInTheDocument();
        });

        it('should fall back to the first authorization when the connection has no OAuth2 authorization', async () => {
            const user = userEvent.setup();

            hoisted.connectionDefinition = {
                authorizations: [
                    {title: 'Basic Auth', type: 'BASIC_AUTH'},
                    {title: 'API Key', type: 'API_KEY'},
                ],
            };

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(await screen.findByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(hoisted.createMutate).toHaveBeenCalledWith(
                    expect.objectContaining({authorizationType: 'BASIC_AUTH'})
                )
            );
        });

        it('should re-apply the default authorization when the integration changes', async () => {
            const user = userEvent.setup();

            const {rerender} = render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Use API Key'}));

            expect(await screen.findByRole('button', {name: 'Save'})).toBeInTheDocument();

            hoisted.connectionDefinition = {
                authorizations: [
                    {title: 'API Key', type: 'API_KEY'},
                    {title: 'OAuth2', type: 'OAUTH2_AUTHORIZATION_CODE'},
                ],
            };
            hoisted.integration = {componentName: 'slack'};

            rerender(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await waitFor(() => expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument());

            expect(screen.getByRole('button', {name: 'Next'})).toBeInTheDocument();
        });

        it('should keep Next disabled until the connection authorizations are loaded', async () => {
            const user = userEvent.setup();

            hoisted.connectionDefinition = undefined;
            hoisted.workflows = [{id: 'workflow-1', workflowUuid: 'workflow-uuid-1'}];

            const {rerender} = render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Pick Integration'}));

            expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

            hoisted.connectionDefinition = {authorizations: [{title: 'API Key', type: 'API_KEY'}]};

            rerender(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await waitFor(() => expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled());
        });

        it('should save the authorization selected in the basic step', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Use API Key'}));

            await user.click(await screen.findByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(hoisted.createMutate).toHaveBeenCalledWith(
                    expect.objectContaining({authorizationType: 'API_KEY'})
                )
            );
        });
    });

    describe('edit mode', () => {
        it('should title the dialog for an existing configuration', () => {
            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={{
                        id: 1,
                        integrationId: 2,
                        integrationVersion: 1,
                        name: 'Existing',
                    }}
                    onClose={onClose}
                />
            );

            expect(screen.getByRole('heading', {name: /Edit Instance Configuration/})).toBeInTheDocument();
        });

        it('should offer Save only with the edit scope', () => {
            const existingConfiguration = {id: 1, integrationId: 2, integrationVersion: 1, name: 'Existing'};

            hoisted.grantedScopes = ['INTEGRATION_INSTANCE_CONFIGURATION_CREATE'];

            const withoutEditRender = render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={existingConfiguration}
                    onClose={onClose}
                />
            );

            expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();

            withoutEditRender.unmount();

            hoisted.grantedScopes = ['INTEGRATION_INSTANCE_CONFIGURATION_EDIT'];

            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={existingConfiguration}
                    onClose={onClose}
                />
            );

            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should show withheld credentials as hidden without the edit scope and never send them back', async () => {
            const user = userEvent.setup();

            const existingConfiguration = {
                authorizationType: 'API_KEY',
                connectionAuthorizationParameters: {value: 'secret-api-key-value'},
                connectionConnectionParameters: {subdomain: 'acme-subdomain'},
                connectionParameters: {subdomain: 'acme-subdomain', value: 'secret-api-key-value'},
                createdBy: 'creator@localhost.com',
                id: 1,
                integrationId: 2,
                integrationVersion: 1,
                name: 'Existing',
            } as unknown as IntegrationInstanceConfiguration;

            act(() => {
                authenticationStore.setState({
                    account: {authorities: ['ROLE_USER'], login: 'creator@localhost.com'} as never,
                    authenticated: true,
                });
            });

            hoisted.connectionDefinition = {authorizations: [{title: 'API Key', type: 'API_KEY'}]};
            hoisted.grantedScopes = ['INTEGRATION_INSTANCE_CONFIGURATION_VIEW'];

            const viewerRender = render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={existingConfiguration}
                    onClose={onClose}
                />
            );

            expect(screen.getByTestId('connection-parameters')).toHaveAttribute('data-credentials-hidden', 'true');

            viewerRender.unmount();

            hoisted.grantedScopes = [
                'INTEGRATION_INSTANCE_CONFIGURATION_EDIT',
                'INTEGRATION_INSTANCE_CONFIGURATION_VIEW',
            ];

            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={existingConfiguration}
                    onClose={onClose}
                />
            );

            expect(screen.getByTestId('connection-parameters')).toHaveAttribute('data-credentials-hidden', 'false');

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

            const payload = hoisted.updateMutate.mock.calls[0][0];

            ['connectionAuthorizationParameters', 'connectionConnectionParameters', 'connectionParameters'].forEach(
                (credentialKey) => expect(payload).not.toHaveProperty(credentialKey)
            );
            expect(JSON.stringify(payload)).not.toContain('secret-api-key-value');
            expect(payload).toMatchObject({id: 1, name: 'Existing'});
        });

        it('should show credentials as hidden to an admin who did not create the configuration', () => {
            const existingConfiguration = {
                authorizationType: 'API_KEY',
                connectionParameters: {value: 'secret-api-key-value'},
                createdBy: 'creator@localhost.com',
                id: 1,
                integrationId: 2,
                integrationVersion: 1,
                name: 'Existing',
            } as unknown as IntegrationInstanceConfiguration;

            act(() => {
                authenticationStore.setState({
                    account: {authorities: ['ROLE_ADMIN'], login: 'admin@localhost.com'} as never,
                    authenticated: true,
                });
            });

            hoisted.connectionDefinition = {authorizations: [{title: 'API Key', type: 'API_KEY'}]};
            hoisted.grantedScopes = [
                'INTEGRATION_INSTANCE_CONFIGURATION_EDIT',
                'INTEGRATION_INSTANCE_CONFIGURATION_VIEW',
            ];

            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={existingConfiguration}
                    onClose={onClose}
                />
            );

            expect(screen.getByTestId('connection-parameters')).toHaveAttribute('data-credentials-hidden', 'true');
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the stored authorization type when saving', async () => {
            const user = userEvent.setup();

            hoisted.connectionDefinition = {
                authorizations: [
                    {title: 'API Key', type: 'API_KEY'},
                    {title: 'OAuth2', type: 'OAUTH2_AUTHORIZATION_CODE'},
                ],
            };
            hoisted.integration = {componentName: 'httpClient'};

            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={{
                        authorizationType: 'API_KEY',
                        id: 1,
                        integrationId: 2,
                        integrationVersion: 1,
                        name: 'Existing',
                    }}
                    onClose={onClose}
                />
            );

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(hoisted.updateMutate).toHaveBeenCalledWith(
                    expect.objectContaining({authorizationType: 'API_KEY'})
                )
            );

            expect(hoisted.createMutate).not.toHaveBeenCalled();
            expect(screen.queryByTestId('oauth2-step')).not.toBeInTheDocument();
        });
    });

    describe('version upgrade', () => {
        it('should title the dialog for a version upgrade', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} updateIntegrationVersion />);

            expect(screen.getByRole('heading', {name: 'Upgrade Integration Version'})).toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close and cancel controls', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of any form submission', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            // closeDialog defers onClose through a setTimeout so the form can reset first.
            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
        });
    });
});
