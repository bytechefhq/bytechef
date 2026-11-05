import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationDialog from '../AutomationWorkflowProjectConfigurationDialog';

const mocks = vi.hoisted(() => ({
    configurations: {current: [] as unknown[]},
    createMutateAsync: vi.fn(),
    enableWorkflowMutateAsync: vi.fn(),
    impactData: {current: {automationWorkflowProjectConfigurationVersionImpact: {workflows: []}} as unknown},
    impactQuery: vi.fn(),
    providableValuesQuery: vi.fn(),
    updateMutateAsync: vi.fn(),
    updateWorkflowMutateAsync: vi.fn(),
    versionWorkflowChangesQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [{name: 'slack', title: 'Slack'}],
    }),
}));

vi.mock('@/shared/queries/platform/environments.queries', () => ({
    useGetEnvironmentsQuery: () => ({
        data: [
            {id: 1, name: 'Development'},
            {id: 2, name: 'Staging'},
        ],
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationVersionImpactQuery: (variables: unknown, options: {enabled: boolean}) => {
        mocks.impactQuery(variables, options);

        return {data: options.enabled ? mocks.impactData.current : undefined};
    },
    useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery: (
        variables: {id: string; workflowUuid: string},
        options: {enabled: boolean}
    ) => {
        mocks.providableValuesQuery(variables, options);

        return {
            data: options.enabled
                ? {
                      automationWorkflowProjectConfigurationWorkflowProvidableValues: {
                          connectionSlots: [],
                          inputs: [
                              {
                                  componentName: null,
                                  componentVersion: null,
                                  groupName: null,
                                  label: 'Region',
                                  name: 'region',
                                  optionConnections: [],
                                  required: false,
                                  type: 'STRING',
                              },
                          ],
                          providedConnections: null,
                          providedInputs: null,
                      },
                  }
                : undefined,
        };
    },
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: mocks.configurations.current},
    }),
    useAutomationWorkflowProjectVersionWorkflowChangesQuery: (
        variables: {id: string; projectVersion: number},
        options: {enabled: boolean}
    ) => {
        mocks.versionWorkflowChangesQuery(variables, options);

        return {
            data: options.enabled
                ? {
                      automationWorkflowProjectVersionWorkflowChanges: {
                          baseVersion: null,
                          version: variables.projectVersion,
                          workflows: [
                              {changeCount: 0, changeType: 'ADDED', label: 'Sync Contacts', workflowUuid: 'workflow-1'},
                              {changeCount: 0, changeType: 'ADDED', label: 'Send Digest', workflowUuid: 'workflow-2'},
                              {changeCount: 0, changeType: 'REMOVED', label: 'Old Flow', workflowUuid: 'workflow-3'},
                          ],
                      },
                  }
                : undefined,
            isLoading: false,
        };
    },
    useAutomationWorkflowProjectVersionsQuery: () => ({
        data: {
            automationWorkflowProjectVersions: [
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 1},
                {description: null, publishedDate: null, status: 'DRAFT', version: 2},
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 3},
            ],
        },
    }),
    useCreateAutomationWorkflowProjectConfigurationMutation: () => ({
        isPending: false,
        mutateAsync: mocks.createMutateAsync,
    }),
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutateAsync: mocks.enableWorkflowMutateAsync,
    }),
    useUpdateAutomationWorkflowProjectConfigurationMutation: () => ({
        isPending: false,
        mutateAsync: mocks.updateMutateAsync,
    }),
    useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutateAsync: mocks.updateWorkflowMutateAsync,
    }),
}));

const project = {
    categoryId: null,
    configurationCount: 0,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
    lastPublishedVersion: 3,
    name: 'My Project',
    permissionExpression: null,
    published: true,
    tagIds: [],
    version: 4,
    workflowTemplates: [],
};

const alphaProject = {...project, id: 'project-alpha', name: 'Alpha Project'};

const betaProject = {...project, id: 'project-beta', name: 'Beta Project'};

const existingConfiguration = {
    automationHubVisible: true,
    connectedUserCount: 0,
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    name: 'PRODUCTION',
    permissionExpression: null,
    projectId: 'project-1',
    projectVersion: 3,
    workflows: [],
};

const existingConfigurationWithWorkflows = {
    ...existingConfiguration,
    workflows: [
        {
            enabled: true,
            label: 'Sync Contacts',
            lastExecutionDate: null,
            providedConnections: null,
            providedInputs: null,
            workflowId: 'workflow-id-1',
            workflowUuid: 'workflow-1',
        },
        {
            enabled: false,
            label: 'Send Digest',
            lastExecutionDate: null,
            providedConnections: null,
            providedInputs: null,
            workflowId: 'workflow-id-2',
            workflowUuid: 'workflow-2',
        },
    ],
};

const expectedCreateVariables = {
    automationHubVisible: true,
    enabled: false,
    environmentId: '2',
    name: 'My Project',
    permissionExpression: null,
    projectId: 'project-1',
    projectVersion: 3,
};

describe('AutomationWorkflowProjectConfigurationDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        environmentStore.setState({currentEnvironmentId: 2});

        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();

        mocks.createMutateAsync.mockResolvedValue({createAutomationWorkflowProjectConfiguration: 'configuration-9'});
        mocks.enableWorkflowMutateAsync.mockResolvedValue({enableAutomationWorkflowProjectConfigurationWorkflow: true});
        mocks.updateMutateAsync.mockResolvedValue({updateAutomationWorkflowProjectConfiguration: true});
        mocks.updateWorkflowMutateAsync.mockResolvedValue({
            updateAutomationWorkflowProjectConfigurationWorkflow: true,
        });
    });

    afterEach(() => {
        mocks.configurations.current = [];
        mocks.impactData.current = {automationWorkflowProjectConfigurationVersionImpact: {workflows: []}};

        resetAll();
        vi.clearAllMocks();
    });

    describe('basic step', () => {
        it('shows the wizard header, the steps and the step progress', () => {
            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            expect(screen.getByText('New Project Configuration')).toBeInTheDocument();
            expect(screen.getAllByText('Run a published project version for connected users.').length).toBeGreaterThan(
                0
            );
            expect(screen.getByRole('navigation', {name: 'Steps'})).toHaveTextContent('BasicWorkflows');
            expect(screen.getAllByText('Step 1 of 2').length).toBeGreaterThan(0);
            expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
        });

        it('keeps a fixed project selected and disabled', () => {
            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            expect(screen.getByRole('combobox', {name: 'Project'})).toBeDisabled();
            expect(screen.getByRole('combobox', {name: 'Project'})).toHaveTextContent('My Project');
        });

        it('prefills the name with the project name and saves from the basic step with defaults', async () => {
            const user = userEvent.setup();

            const onClose = vi.fn();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={onClose} project={project} />
                </TooltipProvider>
            );

            expect(screen.getByText('Staging')).toBeInTheDocument();
            expect(screen.queryByRole('combobox', {name: 'Environment'})).not.toBeInTheDocument();
            expect(screen.getByLabelText(/^Name/)).toHaveValue('My Project');
            expect(screen.getByRole('combobox', {name: 'Version'})).toHaveTextContent('Latest published version (V3)');

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

            expect(mocks.createMutateAsync).toHaveBeenCalledTimes(1);
            expect(mocks.createMutateAsync).toHaveBeenCalledWith(expectedCreateVariables);
            expect(mocks.enableWorkflowMutateAsync).not.toHaveBeenCalled();
            expect(mocks.updateWorkflowMutateAsync).not.toHaveBeenCalled();
        });

        it('creates a configuration with the chosen version', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('combobox', {name: 'Version'}));
            await user.click(await screen.findByRole('option', {name: 'V1'}));

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(mocks.createMutateAsync).toHaveBeenCalledTimes(1));

            expect(mocks.createMutateAsync).toHaveBeenCalledWith({...expectedCreateVariables, projectVersion: 1});
        });

        it('leaves the name empty when the project name is already taken in the environment', () => {
            mocks.configurations.current = [{...existingConfiguration, name: 'my project'}];

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            expect(screen.getByLabelText(/^Name/)).toHaveValue('');
            expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
            expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
        });

        it('updates the prefilled name when the project changes until the user types a name', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        onClose={vi.fn()}
                        projects={[alphaProject, betaProject]}
                    />
                </TooltipProvider>
            );

            expect(screen.getByLabelText(/^Name/)).toHaveValue('');
            expect(screen.getByRole('combobox', {name: 'Version'})).toHaveTextContent('Select a project first');

            await user.click(screen.getByRole('combobox', {name: 'Project'}));
            await user.click(await screen.findByRole('option', {name: 'Alpha Project'}));

            expect(screen.getByLabelText(/^Name/)).toHaveValue('Alpha Project');

            await user.click(screen.getByRole('combobox', {name: 'Project'}));
            await user.click(await screen.findByRole('option', {name: 'Beta Project'}));

            expect(screen.getByLabelText(/^Name/)).toHaveValue('Beta Project');

            await user.clear(screen.getByLabelText(/^Name/));
            await user.type(screen.getByLabelText(/^Name/), 'Custom');

            await user.click(screen.getByRole('combobox', {name: 'Project'}));
            await user.click(await screen.findByRole('option', {name: 'Alpha Project'}));

            expect(screen.getByLabelText(/^Name/)).toHaveValue('Custom');
        });

        it('creates a second configuration in the same environment with a name and permission expression', async () => {
            const user = userEvent.setup();

            mocks.configurations.current = [existingConfiguration];

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            await user.clear(screen.getByLabelText(/^Name/));
            await user.type(screen.getByLabelText(/^Name/), 'Beta');
            await user.click(screen.getByLabelText('Assignment rule'));
            await user.paste("metadata['beta'] == 'true'");

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(mocks.createMutateAsync).toHaveBeenCalledTimes(1));

            expect(mocks.createMutateAsync).toHaveBeenCalledWith({
                ...expectedCreateVariables,
                name: 'Beta',
                permissionExpression: "metadata['beta'] == 'true'",
            });
        });

        it('shows the assignment rule placeholder and help text', () => {
            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            expect(screen.getByPlaceholderText("e.g. metadata['beta'] == 'true'")).toBeInTheDocument();
            expect(
                screen.getByText(
                    /^Which new users self-serve places on this configuration\. Checked once, when a user is first assigned\./
                )
            ).toBeInTheDocument();
        });

        it('rejects a duplicate name in the same environment', async () => {
            const user = userEvent.setup();

            mocks.configurations.current = [existingConfiguration];

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            await user.clear(screen.getByLabelText(/^Name/));
            await user.type(screen.getByLabelText(/^Name/), '  production ');

            expect(screen.getByText('A configuration named production already exists in Staging.')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
            expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
        });

        it('offers only published versions', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('combobox', {name: 'Version'}));

            expect(await screen.findByRole('option', {name: 'V1'})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: 'V3'})).toBeInTheDocument();
            expect(screen.queryByRole('option', {name: 'V2'})).not.toBeInTheDocument();
        });

        it('explains the Automation Hub switch in a tooltip instead of inline text', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            expect(screen.queryByText('Turn off for flows you only activate through the API.')).not.toBeInTheDocument();

            await user.hover(screen.getByLabelText('About Show in Automation Hub'));

            expect(await screen.findByRole('tooltip')).toHaveTextContent(
                'Turn off for flows you only activate through the API.'
            );
        });
    });

    describe('workflows step', () => {
        it('lists the workflows of the chosen version, all enabled, and goes back to the basic step', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('button', {name: 'Next'}));

            expect(await screen.findByRole('switch', {name: 'Sync Contacts'})).toBeChecked();
            expect(screen.getByRole('switch', {name: 'Send Digest'})).toBeChecked();
            expect(screen.queryByText('Old Flow')).not.toBeInTheDocument();
            expect(screen.getAllByText('Step 2 of 2').length).toBeGreaterThan(0);
            expect(screen.queryByLabelText(/^Name/)).not.toBeInTheDocument();
            expect(mocks.versionWorkflowChangesQuery).toHaveBeenLastCalledWith(
                {id: 'project-1', projectVersion: 3},
                {enabled: true}
            );

            await user.click(screen.getByRole('button', {name: /^Basic/}));

            expect(screen.getByLabelText(/^Name/)).toHaveValue('My Project');
        });

        it('saves from the workflows step with only the workflows the admin changed', async () => {
            const user = userEvent.setup();

            const onClose = vi.fn();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={onClose} project={project} />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('button', {name: 'Next'}));
            await user.click(await screen.findByRole('switch', {name: 'Send Digest'}));
            await user.click(screen.getByRole('switch', {name: 'Sync Contacts'}));
            await user.click(screen.getByRole('switch', {name: 'Sync Contacts'}));

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

            expect(mocks.createMutateAsync).toHaveBeenCalledWith(expectedCreateVariables);
            expect(mocks.enableWorkflowMutateAsync).toHaveBeenCalledTimes(1);
            expect(mocks.enableWorkflowMutateAsync).toHaveBeenCalledWith({
                enable: false,
                id: 'configuration-9',
                workflowUuid: 'workflow-2',
            });
            expect(mocks.updateWorkflowMutateAsync).not.toHaveBeenCalled();
            expect(mocks.providableValuesQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});
        });

        it('keeps the dialog open when a workflow change fails and retries without creating again', async () => {
            const user = userEvent.setup();

            const onClose = vi.fn();

            mocks.enableWorkflowMutateAsync.mockRejectedValueOnce(new Error('failed'));

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={onClose} project={project} />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('button', {name: 'Next'}));
            await user.click(await screen.findByRole('switch', {name: 'Send Digest'}));

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(await screen.findByRole('alert')).toHaveTextContent(
                'The configuration was created, but some workflow settings could not be saved.'
            );
            expect(onClose).not.toHaveBeenCalled();

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

            expect(mocks.createMutateAsync).toHaveBeenCalledTimes(1);
            expect(mocks.enableWorkflowMutateAsync).toHaveBeenCalledTimes(2);
            expect(mocks.enableWorkflowMutateAsync).toHaveBeenLastCalledWith({
                enable: false,
                id: 'configuration-9',
                workflowUuid: 'workflow-2',
            });
        });

        it('keeps the dialog open and sends nothing else when creating fails', async () => {
            const user = userEvent.setup();

            const onClose = vi.fn();

            mocks.createMutateAsync.mockRejectedValueOnce(new Error('failed'));

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={onClose} project={project} />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('button', {name: 'Next'}));
            await user.click(await screen.findByRole('switch', {name: 'Send Digest'}));

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(mocks.createMutateAsync).toHaveBeenCalledTimes(1));
            await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());

            expect(mocks.enableWorkflowMutateAsync).not.toHaveBeenCalled();
            expect(onClose).not.toHaveBeenCalled();
            expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        });
    });

    describe('edit', () => {
        it('sends the name and permission expression without offering the environment or version', async () => {
            const user = userEvent.setup();

            const onClose = vi.fn();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        configuration={existingConfiguration}
                        onClose={onClose}
                        project={project}
                    />
                </TooltipProvider>
            );

            expect(screen.getByText('Edit Project Configuration')).toBeInTheDocument();
            expect(screen.getByText('Staging')).toBeInTheDocument();
            expect(screen.getByLabelText(/^Name/)).toHaveValue('PRODUCTION');
            expect(screen.getByRole('combobox', {name: 'Project'})).toBeDisabled();
            expect(screen.queryByRole('combobox', {name: 'Environment'})).not.toBeInTheDocument();
            expect(screen.queryByRole('combobox', {name: 'Version'})).not.toBeInTheDocument();
            expect(screen.queryByRole('switch', {name: 'Enabled'})).not.toBeInTheDocument();

            await user.click(screen.getByRole('switch', {name: 'Show in Automation Hub'}));

            await user.clear(screen.getByLabelText(/^Name/));
            await user.type(screen.getByLabelText(/^Name/), 'Renamed');
            await user.click(screen.getByLabelText('Assignment rule'));
            await user.paste("metadata['beta'] == 'true'");

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

            expect(mocks.updateMutateAsync).toHaveBeenCalledTimes(1);
            expect(mocks.updateMutateAsync).toHaveBeenCalledWith({
                automationHubVisible: false,
                enabled: true,
                id: 'configuration-1',
                name: 'Renamed',
                permissionExpression: "metadata['beta'] == 'true'",
                projectVersion: 3,
            });
            expect(mocks.createMutateAsync).not.toHaveBeenCalled();
            expect(mocks.enableWorkflowMutateAsync).not.toHaveBeenCalled();
            expect(mocks.updateWorkflowMutateAsync).not.toHaveBeenCalled();
            expect(mocks.versionWorkflowChangesQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});
        });

        it('edits the enabled state and provided values of the configuration workflows', async () => {
            const user = userEvent.setup();

            const onClose = vi.fn();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        configuration={existingConfigurationWithWorkflows}
                        onClose={onClose}
                        project={project}
                    />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('button', {name: 'Next'}));

            expect(await screen.findByRole('switch', {name: 'Sync Contacts'})).toBeChecked();
            expect(screen.getByRole('switch', {name: 'Send Digest'})).not.toBeChecked();
            expect(mocks.providableValuesQuery).toHaveBeenCalledWith(
                {id: 'configuration-1', workflowUuid: 'workflow-1'},
                {enabled: true}
            );

            await user.click(screen.getByRole('switch', {name: 'Send Digest'}));
            await user.click(screen.getAllByRole('tab', {name: /Inputs/})[0]);
            await user.type(screen.getAllByLabelText('Region')[0], 'eu');

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

            expect(mocks.updateMutateAsync).toHaveBeenCalledTimes(1);
            expect(mocks.enableWorkflowMutateAsync).toHaveBeenCalledTimes(1);
            expect(mocks.enableWorkflowMutateAsync).toHaveBeenCalledWith({
                enable: true,
                id: 'configuration-1',
                workflowUuid: 'workflow-2',
            });
            expect(mocks.updateWorkflowMutateAsync).toHaveBeenCalledTimes(1);
            expect(mocks.updateWorkflowMutateAsync).toHaveBeenCalledWith({
                id: 'configuration-1',
                providedConnections: {},
                providedInputs: {region: 'eu'},
                workflowUuid: 'workflow-1',
            });
        });
    });

    describe('upgrade project version', () => {
        it('upgrades the version starting from the latest published one, keeping the other settings', async () => {
            const user = userEvent.setup();

            const onClose = vi.fn();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        configuration={{...existingConfiguration, automationHubVisible: false, projectVersion: 1}}
                        onClose={onClose}
                        project={project}
                        updateProjectVersion
                    />
                </TooltipProvider>
            );

            expect(screen.getByText('Upgrade Project Version')).toBeInTheDocument();
            expect(screen.getByText('Staging')).toBeInTheDocument();
            expect(screen.getByRole('combobox', {name: 'Version'})).toHaveTextContent('V3');
            expect(screen.queryByRole('switch', {name: 'Show in Automation Hub'})).not.toBeInTheDocument();
            expect(screen.queryByRole('navigation', {name: 'Steps'})).not.toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

            expect(mocks.updateMutateAsync).toHaveBeenCalledWith({
                automationHubVisible: false,
                enabled: true,
                id: 'configuration-1',
                name: 'PRODUCTION',
                permissionExpression: null,
                projectVersion: 3,
            });
        });

        it('warns that moving to an older version disconnects automations it lacks', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        configuration={existingConfiguration}
                        onClose={vi.fn()}
                        project={project}
                        updateProjectVersion
                    />
                </TooltipProvider>
            );

            const warningText = 'Versions that lack workflows connected users still use cannot be saved.';

            expect(screen.queryByText(warningText)).not.toBeInTheDocument();

            await user.click(screen.getByRole('combobox', {name: 'Version'}));
            await user.click(await screen.findByRole('option', {name: 'V1'}));

            expect(await screen.findByText(warningText)).toBeInTheDocument();
        });
    });

    describe('version impact', () => {
        const impactWorkflows = [
            {
                affectedConnectedUserCount: 4,
                label: 'Sync Contacts',
                newRequiredConnections: ['slack'],
                newRequiredInputs: ['apiKey', 'region'],
                workflowUuid: 'workflow-1',
            },
        ];

        it('lists the workflows a version change will pause', async () => {
            const user = userEvent.setup();

            mocks.impactData.current = {
                automationWorkflowProjectConfigurationVersionImpact: {workflows: impactWorkflows},
            };

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        configuration={{...existingConfiguration, projectVersion: 1}}
                        onClose={vi.fn()}
                        project={project}
                        updateProjectVersion
                    />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('combobox', {name: 'Version'}));
            await user.click(await screen.findByRole('option', {name: 'V3'}));

            expect(
                await screen.findByText(
                    'Version 3 adds required input apiKey to Sync Contacts: 4 connected users will pause until they fill it.'
                )
            ).toBeInTheDocument();
            expect(
                screen.getByText(
                    'Version 3 adds required input region to Sync Contacts: 4 connected users will pause until they fill it.'
                )
            ).toBeInTheDocument();
            expect(
                screen.getByText(
                    'Version 3 adds a required Slack connection to Sync Contacts: connected users without a Slack integration will pause.'
                )
            ).toBeInTheDocument();
        });

        it('renders nothing when the impact list is empty', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        configuration={{...existingConfiguration, projectVersion: 1}}
                        onClose={vi.fn()}
                        project={project}
                        updateProjectVersion
                    />
                </TooltipProvider>
            );

            await user.click(screen.getByRole('combobox', {name: 'Version'}));
            await user.click(await screen.findByRole('option', {name: 'V3'}));

            expect(screen.queryByText(/will pause/)).not.toBeInTheDocument();
        });

        it('renders nothing and does not query when creating a configuration', () => {
            mocks.impactData.current = {
                automationWorkflowProjectConfigurationVersionImpact: {workflows: impactWorkflows},
            };

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
                </TooltipProvider>
            );

            expect(screen.queryByText(/will pause/)).not.toBeInTheDocument();
            expect(mocks.impactQuery).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        });

        it('does not enable the query while the version equals the current one', async () => {
            const user = userEvent.setup();

            render(
                <TooltipProvider>
                    <AutomationWorkflowProjectConfigurationDialog
                        configuration={existingConfiguration}
                        onClose={vi.fn()}
                        project={project}
                        updateProjectVersion
                    />
                </TooltipProvider>
            );

            expect(mocks.impactQuery).toHaveBeenCalled();
            expect(mocks.impactQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});

            await user.click(screen.getByRole('combobox', {name: 'Version'}));
            await user.click(await screen.findByRole('option', {name: 'V1'}));

            expect(mocks.impactQuery).toHaveBeenLastCalledWith(
                {id: 'configuration-1', projectVersion: 1},
                {enabled: true}
            );
        });
    });
});
