import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

export const useProjectsLeftSidebar = ({
    bottomResizablePanelRef,
    projectId,
}: {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle>;
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

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }

            navigate(`/automation/projects/${projectId}/project-workflows/${response.projectWorkflowId}`);
        },
    });

    const getWorkflowsProjectId = (projects: Project[]) => {
        const workflowToProjectMap: Record<number, number> = {};

        projects.forEach((project) => {
            project.projectWorkflowIds?.forEach((workflowId) => {
                workflowToProjectMap[workflowId] = +(project.id || 0);
            });
        });

        return (workflow: Workflow) => workflowToProjectMap[workflow.projectWorkflowId || 0] || 0;
    };

    const getFilteredWorkflows = (workflows: Workflow[] | undefined, sortBy: string, searchValue: string) => {
        if (!workflows) {
            return [];
        }

        const sortFunctions = {
            alphabetical: (a: Workflow, b: Workflow) => (a.label || '').localeCompare(b.label || ''),
            'date-created': (a: Workflow, b: Workflow) =>
                (b.createdDate?.getTime() || 0) - (a.createdDate?.getTime() || 0),
            'last-edited': (a: Workflow, b: Workflow) =>
                (b.lastModifiedDate?.getTime() || 0) - (a.lastModifiedDate?.getTime() || 0),
            'reverse-alphabetical': (a: Workflow, b: Workflow) => (b.label || '').localeCompare(a.label || ''),
        };

        return [...workflows]
            .sort(sortFunctions[sortBy as keyof typeof sortFunctions] || (() => 0))
            .filter((workflow) => (workflow.label || '').toLowerCase().includes(searchValue.toLowerCase()));
    };

    const calculateTimeDifference = (date?: string) => {
        if (!date) {
            return 'Unknown';
        }

        const currentTimestamp = new Date();
        const workflowLastModifiedDate = new Date(date || '');
        const millisecondsDifference = currentTimestamp.getTime() - workflowLastModifiedDate.getTime();

        const seconds = Math.floor(millisecondsDifference / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 7) {
            return `Edited on ${workflowLastModifiedDate.toLocaleDateString()}`;
        }

        if (days > 0) {
            return `${days} day${days !== 1 ? 's' : ''} ago`;
        }

        if (hours > 0) {
            return `${hours} hour${hours !== 1 ? 's' : ''} ago`;
        }

        if (minutes > 0) {
            return `${minutes} minute${minutes !== 1 ? 's' : ''} ago`;
        }

        return `just now`;
    };

    return {
        calculateTimeDifference,
        createProjectWorkflowMutation,
        getFilteredWorkflows,
        getWorkflowsProjectId,
    };
};
