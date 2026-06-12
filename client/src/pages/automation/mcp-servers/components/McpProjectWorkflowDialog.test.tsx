import {McpServer} from '@/shared/middleware/graphql';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpProjectWorkflowDialog from './McpProjectWorkflowDialog';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants — vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    eligibleResult: {data: undefined} as {data: unknown},
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useCreateMcpProjectMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
        useToolEligibleProjectVersionWorkflowsQuery: () => hoisted.eligibleResult,
        useUpdateMcpProjectMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
    };
});

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: []}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

// Replace the project/version selectors with simple buttons that invoke their
// onChange synchronously, so we can drive the create-mode flow deterministically.
vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox',
    () => ({
        default: ({onChange}: {onChange: (item: {label: string; name: string; value: number}) => void}) => (
            <button onClick={() => onChange({label: 'Project', name: 'Project', value: 10})} type="button">
                pick-project
            </button>
        ),
    })
);

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect',
    () => ({
        default: ({onChange}: {onChange: (value: number) => void}) => (
            <button onClick={() => onChange(2)} type="button">
                pick-version
            </button>
        ),
    })
);

const mcpServer = {id: 's1', name: 'Server'} as McpServer;

describe('McpProjectWorkflowDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.eligibleResult.data = undefined;

        Element.prototype.scrollIntoView = vi.fn();
    });

    it('shows an explanatory message and keeps Add disabled when the selected project version has no tool-eligible workflows', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {toolEligibleProjectVersionWorkflows: []};

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await user.click(screen.getByText('pick-project'));
        await user.click(screen.getByText('pick-version'));

        expect(screen.getByText(/No tool-eligible workflows found/i)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add'})).toBeDisabled();
    });

    it('enables Add and submits once a tool-eligible workflow is selected', async () => {
        const user = userEvent.setup();

        hoisted.eligibleResult.data = {
            toolEligibleProjectVersionWorkflows: [{id: 'pw1', workflow: {id: 'w1', label: 'Workflow One'}}],
        };

        render(<McpProjectWorkflowDialog mcpServer={mcpServer} />);

        await user.click(screen.getByText('pick-project'));
        await user.click(screen.getByText('pick-version'));

        const addButton = screen.getByRole('button', {name: 'Add'});

        expect(addButton).toBeDisabled();

        await user.click(screen.getByRole('checkbox'));

        expect(addButton).toBeEnabled();

        await user.click(addButton);

        expect(hoisted.createMutate).toHaveBeenCalledTimes(1);
        expect(hoisted.createMutate.mock.calls[0][0]).toMatchObject({
            input: {mcpServerId: 's1', selectedWorkflowIds: ['w1']},
        });
    });
});
