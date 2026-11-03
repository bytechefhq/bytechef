import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationDialog from '../AutomationWorkflowProjectConfigurationDialog';

const mocks = vi.hoisted(() => ({
    configurations: {current: [] as unknown[]},
    createMutate: vi.fn(),
    impactData: {current: {automationWorkflowProjectConfigurationVersionImpact: {workflows: []}} as unknown},
    impactQuery: vi.fn(),
    updateMutate: vi.fn(),
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
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: mocks.configurations.current},
    }),
    useAutomationWorkflowProjectVersionsQuery: () => ({
        data: {
            automationWorkflowProjectVersions: [
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 1},
                {description: null, publishedDate: null, status: 'DRAFT', version: 2},
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 3},
            ],
        },
    }),
    useCreateAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: mocks.createMutate}),
    useUpdateAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: mocks.updateMutate}),
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

describe('AutomationWorkflowProjectConfigurationDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        environmentStore.setState({currentEnvironmentId: 2});

        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
    });

    afterEach(() => {
        mocks.configurations.current = [];
        mocks.impactData.current = {automationWorkflowProjectConfigurationVersionImpact: {workflows: []}};

        resetAll();
        vi.clearAllMocks();
    });

    it('creates a configuration in the current environment with the chosen version', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
            </TooltipProvider>
        );

        expect(screen.getByText('Staging')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Environment'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('combobox', {name: 'Version'}));
        await user.click(await screen.findByRole('option', {name: 'V3'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.createMutate).toHaveBeenCalledTimes(1));

        expect(mocks.createMutate.mock.calls[0][0]).toEqual({
            automationHubVisible: true,
            enabled: false,
            environmentId: '2',
            name: 'PRODUCTION',
            permissionExpression: null,
            projectId: 'project-1',
            projectVersion: 3,
        });
    });

    it('creates a second configuration in the same environment with a name and permission expression', async () => {
        const user = userEvent.setup();

        mocks.configurations.current = [existingConfiguration];

        render(
            <TooltipProvider>
                <AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />
            </TooltipProvider>
        );

        expect(screen.getByLabelText(/^Name/)).toHaveValue('');

        await user.type(screen.getByLabelText(/^Name/), 'Beta');
        await user.click(screen.getByLabelText('Assignment rule'));
        await user.paste("metadata['beta'] == 'true'");

        await user.click(screen.getByRole('combobox', {name: 'Version'}));
        await user.click(await screen.findByRole('option', {name: 'V3'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.createMutate).toHaveBeenCalledTimes(1));

        expect(mocks.createMutate.mock.calls[0][0]).toEqual({
            automationHubVisible: true,
            enabled: false,
            environmentId: '2',
            name: 'Beta',
            permissionExpression: "metadata['beta'] == 'true'",
            projectId: 'project-1',
            projectVersion: 3,
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

        await user.type(screen.getByLabelText(/^Name/), '  production ');

        expect(screen.getByText('A configuration named production already exists in Staging.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
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

    it('edit sends the name and permission expression without offering the environment or version', async () => {
        const user = userEvent.setup();

        const onClose = vi.fn();

        mocks.updateMutate.mockImplementation((_variables, options: {onSuccess: () => void}) => options.onSuccess());

        render(
            <TooltipProvider>
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={existingConfiguration}
                    onClose={onClose}
                    project={project}
                />
            </TooltipProvider>
        );

        expect(screen.getByText('Edit Configuration')).toBeInTheDocument();
        expect(screen.getByText('Staging')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Environment'})).not.toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Version'})).not.toBeInTheDocument();
        expect(screen.queryByRole('switch', {name: 'Enabled'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('switch', {name: 'Show in Automation Hub'}));

        await user.clear(screen.getByLabelText(/^Name/));
        await user.type(screen.getByLabelText(/^Name/), 'Renamed');
        await user.click(screen.getByLabelText('Assignment rule'));
        await user.paste("metadata['beta'] == 'true'");

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            automationHubVisible: false,
            enabled: true,
            id: 'configuration-1',
            name: 'Renamed',
            permissionExpression: "metadata['beta'] == 'true'",
            projectVersion: 3,
        });
        expect(mocks.createMutate).not.toHaveBeenCalled();

        await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    });

    it('upgrades the version starting from the latest published one, keeping the other settings', async () => {
        const user = userEvent.setup();

        mocks.updateMutate.mockImplementation((_variables, options: {onSuccess: () => void}) => options.onSuccess());

        render(
            <TooltipProvider>
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={{...existingConfiguration, automationHubVisible: false, projectVersion: 1}}
                    onClose={vi.fn()}
                    project={project}
                    updateProjectVersion
                />
            </TooltipProvider>
        );

        expect(screen.getByText('Upgrade Project Version')).toBeInTheDocument();
        expect(screen.getByText('Staging')).toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: 'Version'})).toHaveTextContent('V3');
        expect(screen.queryByRole('switch', {name: 'Show in Automation Hub'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
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

        const configuration = existingConfiguration;

        render(
            <TooltipProvider>
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={configuration}
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
