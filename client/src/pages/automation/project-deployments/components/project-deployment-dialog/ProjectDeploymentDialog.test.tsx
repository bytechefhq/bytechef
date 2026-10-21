import {TooltipProvider} from '@/components/ui/tooltip';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {UseFormSetValue} from 'react-hook-form';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDialog from './ProjectDeploymentDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
    workflows: [
        {
            id: 'workflow-v1',
            label: 'webhook1',
            tasks: [],
            triggers: [],
            workflowUuid: 'webhook-uuid',
        },
    ] as Workflow[],
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectVersionWorkflowsQuery: () => ({data: hoisted.workflows, isPending: false}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useCreateProjectDeploymentMutation: () => ({isPending: false, mutate: hoisted.createMutate}),
    useUpdateProjectDeploymentMutation: () => ({isPending: false, mutate: hoisted.updateMutate}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectDeploymentCreated: vi.fn()}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: ReactNode}) => children,
}));

vi.mock('./ProjectDeploymentDialogBasicStep', () => ({
    default: ({
        onDeploymentSelect,
        projectDeployments,
    }: {
        onDeploymentSelect: (deployment: ProjectDeployment) => void;
        projectDeployments: ProjectDeployment[];
    }) => (
        <>
            {projectDeployments.map((deployment) => (
                <button key={deployment.id} onClick={() => onDeploymentSelect(deployment)} type="button">
                    {`Select ${deployment.name}`}
                </button>
            ))}
        </>
    ),
}));

vi.mock('./ProjectDeploymentDialogWorkflowsStep', () => ({
    default: ({setValue}: {setValue: UseFormSetValue<ProjectDeployment>}) => (
        <button onClick={() => setValue('projectDeploymentWorkflows.0.enabled', true)} type="button">
            Enable webhook1
        </button>
    ),
}));

const projectDeployments: ProjectDeployment[] = [
    {
        enabled: true,
        environmentId: 1,
        id: 11,
        name: 'Production',
        projectDeploymentWorkflows: [],
        projectId: 5,
        projectVersion: 1,
    },
    {
        enabled: true,
        environmentId: 1,
        id: 12,
        name: 'Staging',
        projectDeploymentWorkflows: [],
        projectId: 5,
        projectVersion: 1,
    },
];

beforeEach(() => {
    vi.clearAllMocks();
});

describe('ProjectDeploymentDialog', () => {
    it('submits the workflow id after switching between deployments on the same project version', async () => {
        render(
            <MemoryRouter>
                <TooltipProvider>
                    <ProjectDeploymentDialog
                        projectDeployment={{name: 'Webhooks', projectId: 5}}
                        projectDeployments={projectDeployments}
                        projectDeploymentsLoading={false}
                        showTabs
                    />
                </TooltipProvider>
            </MemoryRouter>
        );

        await userEvent.click(await screen.findByRole('button', {name: 'Select Production'}));
        await userEvent.click(screen.getByRole('button', {name: 'Select Staging'}));
        await userEvent.click(screen.getByRole('button', {name: 'Next'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Enable webhook1'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        const submittedDeployment = hoisted.updateMutate.mock.calls[0][0] as ProjectDeployment;

        expect(submittedDeployment.id).toBe(12);
        expect(submittedDeployment.projectDeploymentWorkflows).toEqual([
            expect.objectContaining({enabled: true, workflowId: 'workflow-v1'}),
        ]);
    });
});
