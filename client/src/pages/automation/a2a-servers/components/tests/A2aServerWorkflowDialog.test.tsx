import {A2aServer} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerWorkflowDialog from '../A2aServerWorkflowDialog';
import {A2aProjectItemType} from '../a2a-project-list/hooks/useA2aProjectList';

type EligibleWorkflowType = {id: string; workflow: {id: string; label: string}};

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    eligibleWorkflows: [] as EligibleWorkflowType[],
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useA2aEligibleProjectVersionWorkflowsQuery: (_variables: unknown, options?: {enabled?: boolean}) => ({
        data: options?.enabled ? {a2aEligibleProjectVersionWorkflows: hoisted.eligibleWorkflows} : undefined,
    }),
    useCreateA2aProjectMutation: () => ({mutate: hoisted.createMutate}),
    useUpdateA2aProjectMutation: () => ({mutate: hoisted.updateMutate}),
}));

vi.mock('@/shared/queries/automation/projects.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projects.queries')>()),
    useGetWorkspaceProjectsQuery: () => ({data: [{id: 5, name: 'Sales project'}]}),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectWorkflows.queries')>()),
    useGetProjectVersionWorkflowsQuery: (_id: number, _version: number, _all: boolean, enabled: boolean) => ({
        data: enabled
            ? [
                  {id: 'workflow-a', inputs: [], label: 'Answer question', tasks: [], triggers: []},
                  {id: 'workflow-b', inputs: [], label: 'Create lead', tasks: [], triggers: []},
              ]
            : undefined,
    }),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectDeployments.queries')>()),
    useGetProjectDeploymentQuery: (_id: number, enabled: boolean) => ({
        data: enabled ? {environmentId: 2, id: 31, projectDeploymentWorkflows: []} : undefined,
    }),
}));

vi.mock('@/shared/queries/automation/connections.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/connections.queries')>()),
    useGetWorkspaceConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/components/ConnectionConfigurationList', () => ({
    default: () => <div data-testid="connection-configuration-list" />,
}));

vi.mock('@/shared/components/InputConfigurationList', () => ({
    default: () => <div data-testid="input-configuration-list" />,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => {
    const state = {currentWorkspaceId: 1};

    return {useWorkspaceStore: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox',
    () => ({
        default: ({onChange}: {onChange: (item?: {label: string; value: number}) => void}) => (
            <button onClick={() => onChange({label: 'Sales project', value: 5})} type="button">
                Pick project
            </button>
        ),
    })
);

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect',
    () => ({
        default: ({onChange}: {onChange: (value: number) => void}) => (
            <>
                <button onClick={() => onChange(1)} type="button">
                    Version 1
                </button>

                <button onClick={() => onChange(2)} type="button">
                    Version 2
                </button>
            </>
        ),
    })
);

const a2aServer = {
    authenticationRequired: true,
    enabled: true,
    environmentId: '2',
    id: '7',
    name: 'Sales agent',
} as A2aServer;

const eligibleWorkflows: EligibleWorkflowType[] = [
    {id: '1', workflow: {id: 'workflow-a', label: 'Answer question'}},
    {id: '2', workflow: {id: 'workflow-b', label: 'Create lead'}},
];

const renderDialog = (a2aProject?: A2aProjectItemType) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <A2aServerWorkflowDialog a2aProject={a2aProject} a2aServer={a2aServer} onOpenChange={vi.fn()} open />
        </QueryClientProvider>
    );

const selectProjectVersion = async (user: ReturnType<typeof userEvent.setup>, versionLabel = 'Version 1') => {
    await user.click(screen.getByRole('button', {name: 'Pick project'}));
    await user.click(await screen.findByRole('button', {name: versionLabel}));
    await user.click(screen.getByRole('button', {name: 'Next'}));
};

describe('A2aServerWorkflowDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.eligibleWorkflows = eligibleWorkflows;
    });

    it('starts in create mode without a project, even when the server already has projects', () => {
        renderDialog();

        expect(screen.getAllByText('Add Workflows')[0]).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Pick project'})).toBeInTheDocument();
    });

    it('clears the form after a project is created so the next one starts empty', async () => {
        const user = userEvent.setup();

        hoisted.createMutate.mockImplementation((_variables, options?: {onSuccess?: () => void}) =>
            options?.onSuccess?.()
        );

        renderDialog();

        await selectProjectVersion(user);
        await user.click(screen.getAllByRole('switch')[0]);
        await user.click(screen.getByRole('button', {name: 'Add'}));

        await waitFor(() => expect(screen.queryByRole('switch')).not.toBeInTheDocument());

        expect(screen.queryByRole('button', {name: 'Version 1'})).not.toBeInTheDocument();
    });

    it('keeps submit disabled until a workflow is checked', async () => {
        const user = userEvent.setup();

        renderDialog();

        await selectProjectVersion(user);

        expect(screen.getByRole('button', {name: 'Add'})).toBeDisabled();

        await user.click(screen.getAllByRole('switch')[0]);

        expect(screen.getByRole('button', {name: 'Add'})).toBeEnabled();
    });

    it('names each workflow switch after its workflow and toggles it from the label', async () => {
        const user = userEvent.setup();

        renderDialog();

        await selectProjectVersion(user);

        const answerQuestionCheckbox = screen.getByRole('switch', {name: 'Answer question'});

        expect(screen.getByRole('switch', {name: 'Create lead'})).not.toBeChecked();
        expect(answerQuestionCheckbox).not.toBeChecked();

        await user.click(screen.getByText('Answer question'));

        expect(answerQuestionCheckbox).toBeChecked();
    });

    it('creates the A2A project with the project and the selected workflows', async () => {
        const user = userEvent.setup();

        renderDialog();

        await selectProjectVersion(user);
        await user.click(screen.getAllByRole('switch')[1]);
        await user.click(screen.getByRole('button', {name: 'Add'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.createMutate.mock.calls[0][0]).toEqual({
            input: {
                a2aServerId: '7',
                projectId: '5',
                projectVersion: 1,
                selectedWorkflowIds: ['workflow-b'],
                workflowConfigurations: [{connections: [], inputs: {}, workflowId: 'workflow-b'}],
            },
        });
        expect(hoisted.updateMutate).not.toHaveBeenCalled();
    });

    it('clears the selected workflows when the project version changes', async () => {
        const user = userEvent.setup();

        renderDialog();

        await selectProjectVersion(user);
        await user.click(screen.getAllByRole('switch')[0]);

        expect(screen.getAllByRole('switch')[0]).toBeChecked();

        await user.click(screen.getByRole('button', {name: /^Basic/}));
        await user.click(screen.getByRole('button', {name: 'Version 2'}));
        await user.click(screen.getByRole('button', {name: 'Next'}));

        expect(screen.getAllByRole('switch')[0]).not.toBeChecked();
        expect(screen.getByRole('button', {name: 'Add'})).toBeDisabled();
    });

    it('removes a workflow from the selection when it is unchecked', async () => {
        const user = userEvent.setup();

        renderDialog();

        await selectProjectVersion(user);
        await user.click(screen.getAllByRole('switch')[0]);
        await user.click(screen.getAllByRole('switch')[1]);
        await user.click(screen.getAllByRole('switch')[0]);
        await user.click(screen.getByRole('button', {name: 'Add'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.createMutate.mock.calls[0][0].input.selectedWorkflowIds).toEqual(['workflow-b']);
    });

    it('shows a notice and disables submit when the project version has no eligible workflows', async () => {
        const user = userEvent.setup();

        hoisted.eligibleWorkflows = [];

        renderDialog();

        await selectProjectVersion(user);

        expect(screen.getByText(/No A2A-eligible workflows found for this project version/)).toBeInTheDocument();
        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add'})).toBeDisabled();
    });

    it('pre-fills the selection in edit mode and updates the existing A2A project', async () => {
        const user = userEvent.setup();

        renderDialog({
            id: '11',
            lastModifiedDate: null,
            projectDeploymentId: '31',
            projectId: '5',
            projectVersion: 2,
            workflowIds: ['workflow-a'],
        });

        await waitFor(() => expect(screen.getAllByRole('switch')[0]).toBeChecked());

        expect(screen.getAllByRole('switch')[1]).not.toBeChecked();

        await user.click(screen.getAllByRole('switch')[1]);
        await user.click(screen.getByRole('button', {name: 'Update'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0]).toEqual({
            id: '11',
            input: {
                selectedWorkflowIds: ['workflow-a', 'workflow-b'],
                workflowConfigurations: [
                    {connections: [], inputs: {}, workflowId: 'workflow-a'},
                    {connections: [], inputs: {}, workflowId: 'workflow-b'},
                ],
            },
        });
        expect(hoisted.createMutate).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', {name: /^Basic/}));

        expect(screen.getByDisplayValue('Sales project')).toBeDisabled();
        expect(screen.getByDisplayValue('v2')).toBeDisabled();
    });
});
