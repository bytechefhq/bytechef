import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

export const useCreateProjectWorkflow = ({
    bottomResizablePanelRef,
    projectId,
}: {
    bottomResizablePanelRef?: RefObject<PanelImperativeHandle | null>;
    projectId: number;
}) => {
    const setShowBottomPanelOpen = useWorkflowEditorStore((state) => state.setShowBottomPanelOpen);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {captureProjectWorkflowCreated} = useAnalytics();

    const queryClient = useQueryClient();

    const navigate = useNavigate();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (response) => {
            captureProjectWorkflowCreated();

            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(projectId),
            });

            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.workflows,
            });

            queryClient.invalidateQueries({
                queryKey: ProjectKeys.filteredProjects({
                    id: currentWorkspaceId!,
                }),
            });

            setShowBottomPanelOpen(false);

            bottomResizablePanelRef?.current?.resize(0);

            navigate(`/automation/projects/${projectId}/project-workflows/${response.projectWorkflowId}`);
        },
    });

    return createProjectWorkflowMutation;
};
