import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationProjectConfigurationList from '../AutomationProjectConfigurationList';

const mocks = vi.hoisted(() => ({
    configurations: [] as unknown[],
    configurationsVariables: undefined as unknown,
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    isLoading: false,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationProjectConfigurationDeleteImpactQuery: () => ({
        data: {automationProjectConfigurationDeleteImpact: {connectedUserCount: 0, referenceWorkflowCount: 0}},
        isError: false,
    }),
    useAutomationProjectConfigurationsQuery: (variables: unknown) => {
        mocks.configurationsVariables = variables;

        return {
            data: mocks.isLoading ? undefined : {automationProjectConfigurations: mocks.configurations},
            isLoading: mocks.isLoading,
        };
    },
    useDeleteAutomationProjectConfigurationMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string}) => {
            mocks.deleteMutate(variables);

            options?.onSuccess?.();
        },
    }),
    useEnableAutomationProjectConfigurationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableAutomationProjectConfigurationWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

const project = {
    categoryId: null,
    configurationCount: 0,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
    lastPublishedVersion: 2,
    name: 'Mail Project',
    permissionExpression: null,
    published: true,
    tagIds: [],
    version: 3,
    workflowTemplates: [],
};

const configuration = {
    automationHubVisible: true,
    connectedUserCount: 0,
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    name: 'STAGING',
    permissionExpression: null,
    projectId: 'project-1',
    projectVersion: 2,
    workflows: [],
};

const renderList = (
    onEdit = vi.fn(),
    projectOverrides: Partial<typeof project> = {},
    onCreate = vi.fn(),
    onPublishProject?: (projectId: string) => void
) =>
    render(
        <TooltipProvider>
            <AutomationProjectConfigurationList
                environmentId={2}
                onCreate={onCreate}
                onEdit={onEdit}
                onPublishProject={onPublishProject}
                onUpdateVersion={vi.fn()}
                project={{...project, ...projectOverrides}}
            />
        </TooltipProvider>
    );

describe('AutomationProjectConfigurationList', () => {
    beforeEach(() => {
        windowResizeObserver();

        mocks.configurations = [];
        mocks.configurationsVariables = undefined;
        mocks.isLoading = false;
    });

    it('loads only the configurations of the given environment', () => {
        renderList();

        expect(mocks.configurationsVariables).toEqual({environmentId: '2', projectId: 'project-1'});
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows the empty state when the project has no configurations', () => {
        renderList();

        expect(screen.getByText('No Configurations')).toBeInTheDocument();
        expect(
            screen.getByText('Create a configuration to run a published version of this project in an environment.')
        ).toBeInTheDocument();
    });

    it('creates a configuration from the empty state', async () => {
        const onCreate = vi.fn();

        renderList(vi.fn(), {}, onCreate);

        await userEvent.setup().click(screen.getByRole('button', {name: 'Create Configuration'}));

        expect(onCreate).toHaveBeenCalledOnce();
    });

    it('asks to publish the project instead of offering creation while it is unpublished', () => {
        renderList(vi.fn(), {published: false});

        expect(screen.getByText('Publish the project to configure it')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();
    });

    it('publishes the project from the empty state while it is unpublished', async () => {
        const onPublishProject = vi.fn();

        renderList(
            vi.fn(),
            {published: false, workflowTemplates: [{workflowUuid: 'workflow-1'}] as typeof project.workflowTemplates},
            vi.fn(),
            onPublishProject
        );

        await userEvent.setup().click(screen.getByRole('button', {name: 'Publish'}));

        expect(onPublishProject).toHaveBeenCalledWith('project-1');
    });

    it('disables publishing from the empty state of a project without workflows', () => {
        renderList(vi.fn(), {published: false, workflowTemplates: []}, vi.fn(), vi.fn());

        expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
    });

    it('offers no publish button when publishing is not available', () => {
        renderList(vi.fn(), {published: false});

        expect(screen.queryByRole('button', {name: 'Publish'})).not.toBeInTheDocument();
    });

    it('shows one skeleton card with a row per workflow while loading instead of the empty state', () => {
        mocks.isLoading = true;

        renderList(vi.fn(), {
            configurationCount: 2,
            workflowTemplates: [{}, {}] as (typeof project)['workflowTemplates'],
        });

        expect(screen.getAllByRole('status', {name: 'Loading Mail Project configurations'})).toHaveLength(1);
        expect(within(screen.getByRole('list', {name: 'Loading workflows'})).getAllByRole('listitem')).toHaveLength(2);
        expect(screen.queryByText('No Configurations')).not.toBeInTheDocument();
    });

    it('shows a single skeleton card however many configurations the project has', () => {
        mocks.isLoading = true;

        renderList(vi.fn(), {configurationCount: 5});

        expect(screen.getAllByRole('status', {name: 'Loading Mail Project configurations'})).toHaveLength(1);
    });

    it('shows neither skeleton cards nor the empty state while loading a project known to have no configurations', () => {
        mocks.isLoading = true;

        renderList();

        expect(screen.queryByRole('status', {name: 'Loading Mail Project configurations'})).not.toBeInTheDocument();
        expect(screen.queryByText('No Configurations')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();
    });

    it('lists the configurations by name instead of the empty state', () => {
        mocks.configurations = [configuration];

        renderList();

        expect(screen.getByText(configuration.name)).toBeInTheDocument();
        expect(screen.queryByText('No Configurations')).not.toBeInTheDocument();
    });

    it('deletes the confirmed configuration by id', async () => {
        const user = userEvent.setup();

        mocks.configurations = [configuration];

        renderList();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete Configuration'}));

        const alertDialog = await screen.findByRole('alertdialog');

        await user.click(within(alertDialog).getByRole('button', {name: 'Delete'}));

        await waitFor(() => expect(mocks.deleteMutate).toHaveBeenCalledTimes(1));

        expect(mocks.deleteMutate.mock.calls[0][0]).toEqual({id: 'configuration-1'});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationProjectConfigurations']});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationProjects']});
    });

    it('edits the chosen configuration', async () => {
        const user = userEvent.setup();

        const onEdit = vi.fn();

        mocks.configurations = [configuration];

        renderList(onEdit);

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Edit Configuration'}));

        expect(onEdit).toHaveBeenCalledWith(configuration);
    });
});
