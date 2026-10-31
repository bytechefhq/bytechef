import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationDialog from '../AutomationWorkflowProjectConfigurationDialog';

const mocks = vi.hoisted(() => ({
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
        data: {automationWorkflowProjectConfigurations: []},
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
    codeWorkflowDraftPublishable: null,
    codeWorkflowLanguage: null,
    codeWorkflowProject: false,
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
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    projectId: 'project-1',
    projectVersion: 3,
    workflows: [],
};

describe('AutomationWorkflowProjectConfigurationDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
    });

    afterEach(() => {
        mocks.impactData.current = {automationWorkflowProjectConfigurationVersionImpact: {workflows: []}};

        resetAll();
        vi.clearAllMocks();
    });

    it('creates a configuration with the chosen environment and version', async () => {
        const user = userEvent.setup();

        render(<AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />);

        await user.click(screen.getByRole('combobox', {name: 'Environment'}));
        await user.click(await screen.findByRole('option', {name: 'Staging'}));

        await user.click(screen.getByRole('combobox', {name: 'Version'}));
        await user.click(await screen.findByRole('option', {name: 'V3'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.createMutate).toHaveBeenCalledTimes(1));

        expect(mocks.createMutate.mock.calls[0][0]).toEqual({
            automationHubVisible: true,
            enabled: false,
            environmentId: '2',
            projectId: 'project-1',
            projectVersion: 3,
        });
    });

    it('offers only published versions', async () => {
        const user = userEvent.setup();

        render(<AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />);

        await user.click(screen.getByRole('combobox', {name: 'Version'}));

        expect(await screen.findByRole('option', {name: 'V1'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'V3'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'V2'})).not.toBeInTheDocument();
    });

    it('edits an existing configuration with the environment fixed', async () => {
        const user = userEvent.setup();

        const onClose = vi.fn();

        mocks.updateMutate.mockImplementation((_variables, options: {onSuccess: () => void}) => options.onSuccess());

        render(
            <AutomationWorkflowProjectConfigurationDialog
                configuration={existingConfiguration}
                onClose={onClose}
                project={project}
            />
        );

        expect(screen.getByText('Edit Configuration')).toBeInTheDocument();

        const environmentSelect = screen.getByRole('combobox', {name: 'Environment'});

        expect(environmentSelect).toBeDisabled();
        expect(environmentSelect).toHaveTextContent('Staging');

        await user.click(screen.getByRole('combobox', {name: 'Version'}));
        await user.click(await screen.findByRole('option', {name: 'V1'}));

        await user.click(screen.getByRole('switch', {name: 'Enabled'}));
        await user.click(screen.getByRole('switch', {name: 'Show in Automation Hub'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            automationHubVisible: false,
            enabled: false,
            id: 'configuration-1',
            projectVersion: 1,
        });
        expect(mocks.createMutate).not.toHaveBeenCalled();

        await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    });

    it('warns that moving to an older version disconnects automations it lacks', async () => {
        const user = userEvent.setup();

        const configuration = {
            automationHubVisible: true,
            enabled: true,
            environmentId: '2',
            id: 'configuration-1',
            lastExecutionDate: null,
            lastModifiedDate: null,
            projectId: 'project-1',
            projectVersion: 3,
            workflows: [],
        };

        render(
            <AutomationWorkflowProjectConfigurationDialog
                configuration={configuration}
                onClose={vi.fn()}
                project={project}
            />
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
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={{...existingConfiguration, projectVersion: 1}}
                    onClose={vi.fn()}
                    project={project}
                />
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
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={{...existingConfiguration, projectVersion: 1}}
                    onClose={vi.fn()}
                    project={project}
                />
            );

            await user.click(screen.getByRole('combobox', {name: 'Version'}));
            await user.click(await screen.findByRole('option', {name: 'V3'}));

            expect(screen.queryByText(/will pause/)).not.toBeInTheDocument();
        });

        it('renders nothing and does not query when creating a configuration', () => {
            mocks.impactData.current = {
                automationWorkflowProjectConfigurationVersionImpact: {workflows: impactWorkflows},
            };

            render(<AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />);

            expect(screen.queryByText(/will pause/)).not.toBeInTheDocument();
            expect(mocks.impactQuery).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        });

        it('does not enable the query while the version equals the current one', async () => {
            const user = userEvent.setup();

            render(
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={existingConfiguration}
                    onClose={vi.fn()}
                    project={project}
                />
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
