import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useProjectHeaderDialogsStore from '../../../stores/useProjectHeaderDialogsStore';
import useEditorGuideActions from '../useEditorGuideActions';

const mocks = vi.hoisted(() => ({
    enableDeployment: vi.fn(),
    enableWorkflow: vi.fn(),
    invalidateQueries: vi.fn(),
    runJob: vi.fn(),
    runJobProps: {} as {onSuccess?: () => void},
    toast: vi.fn(),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useCreateProjectDeploymentWorkflowJobMutation: (props: {onSuccess?: () => void}) => {
        mocks.runJobProps.onSuccess = props.onSuccess;

        return {isPending: false, mutate: mocks.runJob};
    },
    useEnableProjectDeploymentMutation: () => ({isPending: false, mutateAsync: mocks.enableDeployment}),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentWorkflows.mutations', () => ({
    useEnableProjectDeploymentWorkflowMutation: () => ({isPending: false, mutateAsync: mocks.enableWorkflow}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

vi.mock('sonner', () => ({toast: mocks.toast}));

describe('useEditorGuideActions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mocks.enableDeployment.mockResolvedValue(undefined);
        mocks.enableWorkflow.mockResolvedValue(undefined);
        useProjectHeaderDialogsStore.setState({
            deployDialogOpen: false,
            deploymentsSheetState: undefined,
            publishPopoverOpen: false,
        });
    });

    it('enables only the deployment when it is off and the workflow is on', async () => {
        const deployment = {
            enabled: false,
            id: 5,
            name: 'd',
            projectDeploymentWorkflows: [{enabled: true, workflowId: 'w9'}],
        };
        const {result} = renderHook(() => useEditorGuideActions({deployment}));

        await act(async () => {
            result.current.enable();
        });

        expect(mocks.enableDeployment).toHaveBeenCalledWith({enable: true, id: 5});
        expect(mocks.enableWorkflow).not.toHaveBeenCalled();
    });

    it('enables only the workflow with the deployment workflow id when the deployment is on', async () => {
        const deployment = {
            enabled: true,
            id: 5,
            name: 'd',
            projectDeploymentWorkflows: [{enabled: false, workflowId: 'w9'}],
        };
        const {result} = renderHook(() => useEditorGuideActions({deployment}));

        await act(async () => {
            result.current.enable();
        });

        expect(mocks.enableDeployment).not.toHaveBeenCalled();
        expect(mocks.enableWorkflow).toHaveBeenCalledWith({enable: true, id: 5, workflowId: 'w9'});
    });

    it('enables the workflow only after the deployment enable request finished', async () => {
        let finishDeploymentEnable: () => void = () => undefined;

        mocks.enableDeployment.mockReturnValue(
            new Promise<void>((resolve) => {
                finishDeploymentEnable = resolve;
            })
        );

        const deployment = {
            enabled: false,
            id: 5,
            name: 'd',
            projectDeploymentWorkflows: [{enabled: false, workflowId: 'w9'}],
        };
        const {result} = renderHook(() => useEditorGuideActions({deployment}));

        await act(async () => {
            result.current.enable();
        });

        expect(mocks.enableDeployment).toHaveBeenCalledWith({enable: true, id: 5});
        expect(mocks.enableWorkflow).not.toHaveBeenCalled();

        await act(async () => {
            finishDeploymentEnable();
        });

        expect(mocks.enableWorkflow).toHaveBeenCalledWith({enable: true, id: 5, workflowId: 'w9'});
    });

    it('does not enable the workflow when the deployment enable request fails', async () => {
        mocks.enableDeployment.mockRejectedValue(new Error('failed'));

        const deployment = {
            enabled: false,
            id: 5,
            name: 'd',
            projectDeploymentWorkflows: [{enabled: false, workflowId: 'w9'}],
        };
        const {result} = renderHook(() => useEditorGuideActions({deployment}));

        await act(async () => {
            result.current.enable();
        });

        expect(mocks.enableWorkflow).not.toHaveBeenCalled();
    });

    it('enables both the deployment and the workflow when both are off', async () => {
        const deployment = {
            enabled: false,
            id: 5,
            name: 'd',
            projectDeploymentWorkflows: [{enabled: false, workflowId: 'w9'}],
        };
        const {result} = renderHook(() => useEditorGuideActions({deployment}));

        await act(async () => {
            result.current.enable();
        });

        expect(mocks.enableDeployment).toHaveBeenCalledWith({enable: true, id: 5});
        expect(mocks.enableWorkflow).toHaveBeenCalledWith({enable: true, id: 5, workflowId: 'w9'});
    });

    it('skips deployment workflows without a workflow id', async () => {
        const deployment = {enabled: true, id: 5, name: 'd', projectDeploymentWorkflows: [{enabled: false}]};
        const {result} = renderHook(() => useEditorGuideActions({deployment}));

        await act(async () => {
            result.current.enable();
        });

        expect(mocks.enableWorkflow).not.toHaveBeenCalled();
    });

    it('shows a toast and invalidates both queries after the job is created', () => {
        const deployment = {
            enabled: true,
            id: 5,
            name: 'd',
            projectDeploymentWorkflows: [{enabled: true, workflowId: 'w9'}],
        };

        renderHook(() => useEditorGuideActions({deployment}));

        act(() => mocks.runJobProps.onSuccess?.());

        expect(mocks.toast).toHaveBeenCalledWith('Workflow request sent.');
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationGettingStarted']});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ProjectDeploymentKeys.projectDeployments});
    });

    it('runs the deployment workflow job', () => {
        const deployment = {
            enabled: true,
            id: 5,
            name: 'd',
            projectDeploymentWorkflows: [{enabled: true, workflowId: 'w9'}],
        };
        const {result} = renderHook(() => useEditorGuideActions({deployment}));

        act(() => result.current.runNow());

        expect(mocks.runJob).toHaveBeenCalledWith({id: 5, workflowId: 'w9'});
    });

    it('does nothing without a deployment', () => {
        const {result} = renderHook(() => useEditorGuideActions({}));

        act(() => {
            result.current.enable();
            result.current.runNow();
        });

        expect(mocks.enableDeployment).not.toHaveBeenCalled();
        expect(mocks.enableWorkflow).not.toHaveBeenCalled();
        expect(mocks.runJob).not.toHaveBeenCalled();
    });

    it('opens the deployments sheet and the executions of the deployed workflow', () => {
        useWorkflowDataStore.setState({
            workflow: {
                ...useWorkflowDataStore.getState().workflow,
                label: 'Learn ByteChef by doing',
                workflowUuid: 'uuid-1',
            },
        });

        const {result} = renderHook(() =>
            useEditorGuideActions({
                deployment: {
                    id: 9,
                    name: 'Getting started',
                    projectDeploymentWorkflows: [{workflowId: 'workflow-v1'}],
                    projectId: 5,
                    projectVersion: 1,
                },
            })
        );

        act(() => result.current.openExecutions());

        expect(useProjectHeaderDialogsStore.getState().deploymentsSheetState).toEqual({
            expandedProjectDeploymentId: 9,
        });

        const workflowSheetState = useProjectDeploymentWorkflowSheetStore.getState();

        expect(workflowSheetState.projectDeploymentWorkflowSheetOpen).toBe(true);
        expect(workflowSheetState.projectDeploymentId).toBe(9);
        expect(workflowSheetState.projectVersion).toBe(1);
        expect(workflowSheetState.workflow).toEqual({
            id: 'workflow-v1',
            label: 'Learn ByteChef by doing',
            workflowUuid: 'uuid-1',
        });
    });

    it('does not open executions without a deployed workflow', () => {
        useProjectDeploymentWorkflowSheetStore.setState({projectDeploymentWorkflowSheetOpen: false});

        const {result} = renderHook(() => useEditorGuideActions({deployment: {id: 9, name: 'Getting started'}}));

        act(() => result.current.openExecutions());

        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(false);
        expect(useProjectHeaderDialogsStore.getState().deploymentsSheetState).toBeUndefined();
    });

    it('opens the publish popover and deploy dialog', () => {
        const {result} = renderHook(() => useEditorGuideActions({}));

        act(() => {
            result.current.openPublish();
            result.current.openDeploy();
        });

        expect(useProjectHeaderDialogsStore.getState().publishPopoverOpen).toBe(true);
        expect(useProjectHeaderDialogsStore.getState().deployDialogOpen).toBe(true);
    });
});
