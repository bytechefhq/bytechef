import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationWorkflowListItem from '@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowListItem';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    integrationWorkflowsQueryResult: {data: null as unknown, isFetching: false},
    invalidateQueries: vi.fn(),
    permissionExpressionMutate: vi.fn(),
    permissionExpressionMutationOptions: {} as {onSuccess?: () => void},
    updateWorkflowMutate: vi.fn(),
    updateWorkflowMutationOptions: {} as {onSuccess?: () => void},
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
    };
});

vi.mock('react-router-dom', () => ({
    Link: ({children, to}: {children: React.ReactNode; to: string}) => <a href={to}>{children}</a>,
    useSearchParams: () => [new URLSearchParams(''), vi.fn()],
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useDeleteWorkflowMutation: () => ({mutate: vi.fn()}),
    useUpdateWorkflowMutation: (options: {onSuccess?: () => void}) => {
        hoisted.updateWorkflowMutationOptions = options;

        return {mutate: hoisted.updateWorkflowMutate};
    },
}));

vi.mock('@/ee/shared/queries/embedded/workflows.queries', () => ({
    WorkflowKeys: {workflow: (id: string) => ['workflow', id]},
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    IntegrationWorkflowKeys: {
        integrationWorkflow: (id: number, workflowId: number) => ['integrationWorkflows', id, workflowId],
        integrationWorkflows: (id: number) => ['integrationWorkflows', id],
    },
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {integrations: ['integrations']},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useIntegrationWorkflowsByIntegrationIdQuery: () => hoisted.integrationWorkflowsQueryResult,
    useUpdateIntegrationWorkflowPermissionExpressionMutation: (options: {onSuccess?: () => void}) => {
        hoisted.permissionExpressionMutationOptions = options;

        return {mutate: hoisted.permissionExpressionMutate};
    },
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    WorkflowTestConfigurationKeys: {workflowTestConfiguration: (id: string) => ['workflowTestConfiguration', id]},
}));

vi.mock('@/shared/components/WorkflowComponentsList', () => ({
    default: ({filteredComponentNames}: {filteredComponentNames: string[]}) => (
        <div data-testid="workflow-components-list">{filteredComponentNames.join(',')}</div>
    ),
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: ({
        additionalContent,
        onSave,
        saveDisabled,
        updateWorkflowMutation,
    }: {
        additionalContent?: ReactNode;
        onSave?: () => void;
        saveDisabled?: boolean;
        updateWorkflowMutation: {mutate: (variables: object) => void};
    }) => (
        <div data-testid="workflow-dialog">
            {additionalContent}

            <button
                disabled={saveDisabled}
                onClick={() => {
                    updateWorkflowMutation.mutate({});

                    onSave?.();
                }}
            >
                Save
            </button>
        </div>
    ),
}));

vi.mock('@/shared/components/DeleteWorkflowAlertDialog', () => ({
    default: () => <div data-testid="delete-workflow-dialog" />,
}));

const integration = {id: 1, name: 'Mailchimp'} as Integration;

const componentDefinitions = {
    gmail: {icon: '<svg />', name: 'gmail', title: 'Gmail', version: 1},
    manual: {icon: '<svg />', name: 'manual', title: 'Manual', version: 1},
    slack: {icon: '<svg />', name: 'slack', title: 'Slack', version: 1},
};

const renderItem = (workflow: Partial<Workflow>) =>
    render(
        <TooltipProvider>
            <IntegrationWorkflowListItem
                filteredComponentNames={workflow.workflowTaskComponentNames ?? []}
                integration={integration}
                workflow={
                    {
                        integrationWorkflowId: 10,
                        label: 'My Workflow',
                        ...workflow,
                    } as Workflow
                }
                workflowComponentDefinitions={componentDefinitions}
                workflowTaskDispatcherDefinitions={{}}
            />
        </TooltipProvider>
    );

const openEditDialog = async () => {
    const menuTrigger = screen.getAllByRole('button').find((button) => button.getAttribute('aria-haspopup') === 'menu');

    await userEvent.click(menuTrigger!);

    await userEvent.click(await screen.findByText('Edit'));
};

describe('IntegrationWorkflowListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.integrationWorkflowsQueryResult = {data: null, isFetching: false};
    });

    it('shows the trigger as its own chip, matching the projects list', () => {
        renderItem({
            triggers: [{label: 'manual', name: 'trigger_1', type: 'manual/v1/manual'}],
            workflowTaskComponentNames: ['manual', 'slack', 'gmail'],
            workflowTriggerComponentNames: ['manual'],
        });

        expect(screen.getByText('manual')).toBeInTheDocument();
    });

    it('hands the shared components list only the task components, not the trigger', () => {
        renderItem({
            triggers: [{label: 'manual', name: 'trigger_1', type: 'manual/v1/manual'}],
            workflowTaskComponentNames: ['manual', 'slack', 'gmail'],
            workflowTriggerComponentNames: ['manual'],
        });

        expect(screen.getByTestId('workflow-components-list').textContent).toBe('slack,gmail');
    });

    it('renders no trigger chip for a workflow that declares none', () => {
        renderItem({workflowTaskComponentNames: ['slack']});

        expect(screen.getByTestId('workflow-components-list').textContent).toBe('slack');
        expect(screen.queryByText('Unknown Trigger')).not.toBeInTheDocument();
    });

    it('falls back to the trigger component title when the trigger carries no label', () => {
        renderItem({
            triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
            workflowTaskComponentNames: ['manual', 'slack'],
            workflowTriggerComponentNames: ['manual'],
        });

        expect(screen.getByText('Manual')).toBeInTheDocument();
    });

    describe('editing the permission expression', () => {
        const loadedIntegrationWorkflows = {
            integrationWorkflowsByIntegrationId: [
                {integrationWorkflowId: '10', permissionExpression: "metadata['tier'] == 'gold'"},
            ],
        };

        it('keeps Save disabled until the stored expression has loaded', async () => {
            hoisted.integrationWorkflowsQueryResult = {data: null, isFetching: true};

            renderItem({id: '5'});

            await openEditDialog();

            const saveButton = screen.getByText('Save');

            expect(saveButton).toBeDisabled();

            await userEvent.click(saveButton);

            expect(hoisted.updateWorkflowMutate).not.toHaveBeenCalled();
            expect(hoisted.permissionExpressionMutate).not.toHaveBeenCalled();
        });

        it('saves the loaded expression only after the workflow update succeeds', async () => {
            hoisted.integrationWorkflowsQueryResult = {data: loadedIntegrationWorkflows, isFetching: false};

            renderItem({id: '5'});

            await openEditDialog();

            expect(screen.getByRole('textbox')).toHaveValue("metadata['tier'] == 'gold'");

            await userEvent.click(screen.getByText('Save'));

            expect(hoisted.updateWorkflowMutate).toHaveBeenCalled();
            expect(hoisted.permissionExpressionMutate).not.toHaveBeenCalled();

            act(() => {
                hoisted.updateWorkflowMutationOptions.onSuccess?.();
            });

            expect(hoisted.permissionExpressionMutate).toHaveBeenCalledWith({
                integrationWorkflowId: '10',
                permissionExpression: "metadata['tier'] == 'gold'",
            });
        });

        it('does not reseed the field over an edit when the expression query refetches', async () => {
            hoisted.integrationWorkflowsQueryResult = {data: loadedIntegrationWorkflows, isFetching: false};

            const {rerender} = renderItem({id: '5'});

            await openEditDialog();

            const textbox = screen.getByRole('textbox');

            await userEvent.clear(textbox);
            await userEvent.type(textbox, 'edited');

            hoisted.integrationWorkflowsQueryResult = {
                data: {
                    integrationWorkflowsByIntegrationId: [
                        {integrationWorkflowId: '10', permissionExpression: 'refetched'},
                    ],
                },
                isFetching: false,
            };

            rerender(
                <TooltipProvider>
                    <IntegrationWorkflowListItem
                        integration={integration}
                        workflow={{id: '5', integrationWorkflowId: 10, label: 'My Workflow'} as Workflow}
                        workflowComponentDefinitions={componentDefinitions}
                        workflowTaskDispatcherDefinitions={{}}
                    />
                </TooltipProvider>
            );

            expect(screen.getByRole('textbox')).toHaveValue('edited');
        });

        it('invalidates the stored expression once the permission update succeeds', () => {
            renderItem({id: '5'});

            hoisted.permissionExpressionMutationOptions.onSuccess?.();

            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['integrationWorkflowsByIntegrationId', {integrationId: '1'}],
            });
        });
    });
});
