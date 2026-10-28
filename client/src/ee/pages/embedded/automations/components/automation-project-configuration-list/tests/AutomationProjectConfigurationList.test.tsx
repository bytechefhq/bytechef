import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationProjectConfigurationList from '../AutomationProjectConfigurationList';

const mocks = vi.hoisted(() => ({
    configurations: [] as unknown[],
    configurationsVariables: undefined as unknown,
    deleteMutate: vi.fn(),
    error: null as Error | null,
    invalidateQueries: vi.fn(),
    isLoading: false,
    refetch: vi.fn(),
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
            data: mocks.isLoading || mocks.error ? undefined : {automationProjectConfigurations: mocks.configurations},
            error: mocks.error,
            isLoading: mocks.isLoading,
            refetch: mocks.refetch,
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
    codeWorkflowDraftPublishable: null,
    codeWorkflowLanguage: null,
    codeWorkflowProject: false,
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
        mocks.error = null;
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

    it('shows the empty state of the listed environment when the project has no configurations', () => {
        renderList();

        expect(screen.getByRole('heading', {name: 'No configurations in Production'})).toBeInTheDocument();
        expect(screen.getByText('Configure this project to run its workflows here.')).toBeInTheDocument();
    });

    it('names the Development environment in the empty state of development configurations', () => {
        render(
            <TooltipProvider>
                <AutomationProjectConfigurationList
                    environmentId={0}
                    onCreate={vi.fn()}
                    onEdit={vi.fn()}
                    onUpdateVersion={vi.fn()}
                    project={project}
                />
            </TooltipProvider>
        );

        expect(screen.getByRole('heading', {name: 'No configurations in Development'})).toBeInTheDocument();
    });

    it('shows a load error with a retry instead of the empty state when the configurations fail to load', async () => {
        mocks.error = new Error('failed');

        renderList();

        expect(screen.getByRole('heading', {name: 'Failed to load configurations'})).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: /No configurations/})).not.toBeInTheDocument();

        await userEvent.setup().click(screen.getByRole('button', {name: 'Retry'}));

        expect(mocks.refetch).toHaveBeenCalledOnce();
    });

    it('creates a configuration from the empty state', async () => {
        const onCreate = vi.fn();

        renderList(vi.fn(), {}, onCreate);

        await userEvent.setup().click(screen.getByRole('button', {name: 'Configure'}));

        expect(onCreate).toHaveBeenCalledOnce();
    });

    it('asks to publish the project instead of offering creation while it is unpublished', () => {
        renderList(vi.fn(), {published: false});

        expect(screen.getByRole('heading', {name: 'Publish the project to configure it'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Configure'})).not.toBeInTheDocument();
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
        expect(screen.queryByText(/No configurations/)).not.toBeInTheDocument();
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
        expect(screen.queryByText(/No configurations/)).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Configure'})).not.toBeInTheDocument();
    });

    it('lists the configurations by name instead of the empty state', () => {
        mocks.configurations = [configuration];

        renderList();

        expect(screen.getByText(configuration.name)).toBeInTheDocument();
        expect(screen.queryByText(/No configurations/)).not.toBeInTheDocument();
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
