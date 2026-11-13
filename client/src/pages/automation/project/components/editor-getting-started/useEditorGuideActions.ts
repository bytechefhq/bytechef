import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useProjectHeaderDialogsStore from '@/pages/automation/project/stores/useProjectHeaderDialogsStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {useEnableProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {
    useCreateProjectDeploymentWorkflowJobMutation,
    useEnableProjectDeploymentMutation,
} from '@/shared/mutations/automation/projectDeployments.mutations';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {WorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';
import {toast} from 'sonner';

interface UseEditorGuideActionsProps {
    deployment?: ProjectDeployment;
}

interface UseEditorGuideActionsI {
    enable: () => void;
    enablePending: boolean;
    openDeploy: () => void;
    openExecutions: () => void;
    openPublish: () => void;
    runNow: () => void;
    runPending: boolean;
    runWaiting: boolean;
}

export default function useEditorGuideActions({deployment}: UseEditorGuideActionsProps): UseEditorGuideActionsI {
    const setDeployDialogOpen = useProjectHeaderDialogsStore((state) => state.setDeployDialogOpen);
    const setDeploymentsSheetState = useProjectHeaderDialogsStore((state) => state.setDeploymentsSheetState);
    const openProjectDeploymentWorkflowSheet = useProjectDeploymentWorkflowSheetStore(
        (state) => state.openProjectDeploymentWorkflowSheet
    );
    const editorWorkflow = useWorkflowDataStore((state) => state.workflow);
    const setPublishPopoverOpen = useProjectHeaderDialogsStore((state) => state.setPublishPopoverOpen);

    const queryClient = useQueryClient();

    const invalidate = useCallback(() => {
        void queryClient.invalidateQueries({queryKey: ['automationGettingStarted']});
        void queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
        void queryClient.invalidateQueries({queryKey: WorkflowExecutionKeys.workflowExecutions});
    }, [queryClient]);

    const enableDeploymentMutation = useEnableProjectDeploymentMutation({onSuccess: invalidate});
    const enableDeploymentWorkflowMutation = useEnableProjectDeploymentWorkflowMutation({onSuccess: invalidate});
    const runJobMutation = useCreateProjectDeploymentWorkflowJobMutation({
        onSuccess: () => {
            toast('Workflow request sent.');

            invalidate();
        },
    });

    const enable = useCallback(() => {
        if (!deployment?.id) {
            return;
        }

        const deploymentId = deployment.id;

        const enableSequentially = async () => {
            if (!deployment.enabled) {
                await enableDeploymentMutation.mutateAsync({enable: true, id: deploymentId});
            }

            for (const deploymentWorkflow of deployment.projectDeploymentWorkflows ?? []) {
                const workflowId = deploymentWorkflow.workflowId;

                if (!deploymentWorkflow.enabled && workflowId) {
                    await enableDeploymentWorkflowMutation.mutateAsync({enable: true, id: deploymentId, workflowId});
                }
            }
        };

        enableSequentially().catch(() => undefined);
    }, [deployment, enableDeploymentMutation, enableDeploymentWorkflowMutation]);

    const openDeploy = useCallback(() => setDeployDialogOpen(true), [setDeployDialogOpen]);

    const openExecutions = useCallback(() => {
        const workflowId = deployment?.projectDeploymentWorkflows?.[0]?.workflowId;

        if (!deployment?.id || !workflowId) {
            return;
        }

        setDeploymentsSheetState({expandedProjectDeploymentId: deployment.id});

        openProjectDeploymentWorkflowSheet({
            projectDeploymentId: deployment.id,
            projectId: deployment.projectId,
            projectName: deployment.name,
            projectVersion: deployment.projectVersion,
            workflow: {
                id: workflowId,
                label: editorWorkflow.label,
                workflowUuid: editorWorkflow.workflowUuid,
            } as Workflow,
        });
    }, [deployment, editorWorkflow, openProjectDeploymentWorkflowSheet, setDeploymentsSheetState]);

    const openPublish = useCallback(() => setPublishPopoverOpen(true), [setPublishPopoverOpen]);

    const runNow = useCallback(() => {
        const workflowId = deployment?.projectDeploymentWorkflows?.[0]?.workflowId;

        if (!deployment?.id || !workflowId) {
            return;
        }

        runJobMutation.mutate({id: deployment.id, workflowId});
    }, [deployment, runJobMutation]);

    return {
        enable,
        enablePending: enableDeploymentMutation.isPending || enableDeploymentWorkflowMutation.isPending,
        openDeploy,
        openExecutions,
        openPublish,
        runNow,
        runPending: runJobMutation.isPending,
        runWaiting: runJobMutation.isPending || runJobMutation.isSuccess,
    };
}
