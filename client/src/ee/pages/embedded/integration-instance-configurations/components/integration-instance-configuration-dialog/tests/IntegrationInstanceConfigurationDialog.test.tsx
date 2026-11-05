import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialog from '../IntegrationInstanceConfigurationDialog';

const hoisted = vi.hoisted(() => ({
    connectionDefinition: undefined as {authorizations: {title: string; type: string}[]} | undefined,
    createMutate: vi.fn(),
    integration: undefined as {componentName: string} | undefined,
    updateMutate: vi.fn(),
    workflows: [] as {id: string; workflowUuid: string}[],
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
        getValues,
        setCurIntegrationId,
        setCurIntegrationVersion,
        setValue,
    }: {
        authorizations?: {type: string}[];
        getValues: (name: string) => unknown;
        setCurIntegrationId: (integrationId: number) => void;
        setCurIntegrationVersion: (integrationVersion: number) => void;
        setValue: (name: string, value: unknown) => void;
    }) => (
        <div data-testid="basic-step">
            <span data-testid="basic-step-authorizations">
                {authorizations?.map((authorization) => authorization.type).join(',')}
            </span>

            <span data-testid="basic-step-permission-expression">
                {String(getValues('permissionExpression') ?? '')}
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
    default: () => <div data-testid="connection-parameters" />,
}));

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();

    hoisted.connectionDefinition = undefined;
    hoisted.integration = undefined;
    hoisted.workflows = [];
});

describe('IntegrationInstanceConfigurationDialog', () => {
    describe('create mode', () => {
        it('should title the dialog for a new configuration and name the current step', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('dialog', {name: 'New Instance Configuration - Basic'})).toBeInTheDocument();
            expect(screen.getByRole('heading', {name: 'New Instance Configuration - Basic'})).toBeInTheDocument();
        });

        it('should show the wizard header with the subtitle, the steps and the step progress', () => {
            hoisted.workflows = [{id: 'workflow-1', workflowUuid: 'workflow-uuid-1'}];

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByText('New Instance Configuration')).toBeInTheDocument();
            expect(screen.getAllByText('Run an integration version for connected users.').length).toBeGreaterThan(0);
            expect(screen.getByRole('navigation', {name: 'Steps'})).toHaveTextContent('BasicWorkflows');
            expect(screen.getByRole('progressbar', {name: 'Step 1 of 2'})).toBeInTheDocument();
        });

        it('should replace the old thin progress bar', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.queryByRole('navigation', {name: 'Progress'})).not.toBeInTheDocument();
        });

        it('should list only the basic step and offer Save when the integration has no workflows', () => {
            hoisted.connectionDefinition = {authorizations: [{title: 'API Key', type: 'API_KEY'}]};
            hoisted.integration = {componentName: 'httpClient'};

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByRole('navigation', {name: 'Steps'})).toHaveTextContent(/^Basic$/);
            expect(screen.getByRole('progressbar', {name: 'Step 1 of 1'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();
        });

        it('should move to the workflows step, go back with Previous and save from the last step', async () => {
            const user = userEvent.setup();

            hoisted.connectionDefinition = {authorizations: [{title: 'API Key', type: 'API_KEY'}]};
            hoisted.integration = {componentName: 'httpClient'};
            hoisted.workflows = [{id: 'workflow-1', workflowUuid: 'workflow-uuid-1'}];

            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.queryByRole('button', {name: 'Previous'})).not.toBeInTheDocument();

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            expect(await screen.findByTestId('workflows-step')).toBeInTheDocument();
            expect(screen.getByRole('progressbar', {name: 'Step 2 of 2'})).toBeInTheDocument();
            expect(screen.getByRole('heading', {name: 'New Instance Configuration - Workflows'})).toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Previous'}));

            expect(await screen.findByTestId('basic-step')).toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Next'}));

            await user.click(await screen.findByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(hoisted.createMutate).toHaveBeenCalledWith(
                    expect.objectContaining({authorizationType: 'API_KEY', environmentId: 1})
                )
            );
        });

        it('should render the basic step', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} />);

            expect(screen.getByTestId('basic-step')).toBeInTheDocument();
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

            expect(screen.getByRole('navigation', {name: 'Steps'})).toHaveTextContent('BasicOAuth2 Connection');

            await user.click(await screen.findByRole('button', {name: 'Next'}));

            expect(await screen.findByTestId('oauth2-step')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
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

            expect(screen.getByRole('heading', {name: 'Edit Instance Configuration'})).toBeInTheDocument();
            expect(screen.queryByRole('navigation', {name: 'Steps'})).not.toBeInTheDocument();
            expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();
        });

        it('should show the connection parameters of an existing configuration', () => {
            hoisted.connectionDefinition = {authorizations: [{title: 'API Key', type: 'API_KEY'}]};

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

            expect(screen.getByTestId('connection-parameters')).toBeInTheDocument();
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

    describe('permission expression', () => {
        it('should send the stored permission expression back when saving an edit', async () => {
            const user = userEvent.setup();

            render(
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={{
                        id: 1,
                        integrationId: 2,
                        integrationVersion: 1,
                        name: 'Existing',
                        permissionExpression: "metadata['beta'] == 'true'",
                    }}
                    onClose={onClose}
                />
            );

            expect(screen.getByTestId('basic-step-permission-expression')).toHaveTextContent(
                "metadata['beta'] == 'true'"
            );

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(hoisted.updateMutate).toHaveBeenCalledWith(
                    expect.objectContaining({permissionExpression: "metadata['beta'] == 'true'"})
                )
            );
        });
    });

    describe('version upgrade', () => {
        it('should title the dialog for a version upgrade', () => {
            render(<IntegrationInstanceConfigurationDialog onClose={onClose} updateIntegrationVersion />);

            expect(screen.getByRole('heading', {name: 'Upgrade Integration Version - Basic'})).toBeInTheDocument();
            expect(
                screen.getAllByText('Move this configuration to another integration version.').length
            ).toBeGreaterThan(0);
        });

        it('should step from the version to the workflows and save the upgrade', async () => {
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
                    updateIntegrationVersion
                />
            );

            expect(screen.getByRole('navigation', {name: 'Steps'})).toHaveTextContent('BasicWorkflows');
            expect(screen.queryByTestId('connection-parameters')).not.toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Next'}));

            expect(await screen.findByTestId('workflows-step')).toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(hoisted.updateMutate).toHaveBeenCalledWith(
                    expect.objectContaining({authorizationType: 'API_KEY', id: 1})
                )
            );

            expect(hoisted.createMutate).not.toHaveBeenCalled();
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
