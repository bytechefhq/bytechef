import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationList from '../AutomationWorkflowProjectConfigurationList';

const mocks = vi.hoisted(() => ({
    configurations: [] as unknown[],
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
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
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: mocks.configurations},
    }),
    useDeleteAutomationWorkflowProjectConfigurationMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string}) => {
            mocks.deleteMutate(variables);

            options?.onSuccess?.();
        },
    }),
    useEnableAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
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
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    projectId: 'project-1',
    projectVersion: 2,
    workflows: [],
};

const renderList = (onEdit = vi.fn(), projectOverrides: Partial<typeof project> = {}, onCreate = vi.fn()) =>
    render(
        <TooltipProvider>
            <AutomationWorkflowProjectConfigurationList
                onCreate={onCreate}
                onEdit={onEdit}
                project={{...project, ...projectOverrides}}
            />
        </TooltipProvider>
    );

describe('AutomationWorkflowProjectConfigurationList', () => {
    beforeEach(() => {
        windowResizeObserver();

        mocks.configurations = [];
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

    it('lists the configurations by environment name instead of the empty state', () => {
        mocks.configurations = [configuration];

        renderList();

        expect(screen.getByText('Staging')).toBeInTheDocument();
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
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationWorkflowProjectConfigurations']});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationWorkflowProjects']});
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
