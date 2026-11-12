import {clearWorkflowHistory} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {AutomationProjectsQuery, useRestoreAutomationProjectVersionMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface UseRestoreAutomationProjectVersionProps {
    currentWorkflowUuid: string;
    projectId?: string;
}

export const useRestoreAutomationProjectVersion = ({
    currentWorkflowUuid,
    projectId,
}: UseRestoreAutomationProjectVersionProps) => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const restoreProjectVersionMutation = useRestoreAutomationProjectVersionMutation();

    const handleRestoreProjectVersion = (projectVersion: number, onSuccess: () => void) => {
        if (!projectId) {
            return;
        }

        restoreProjectVersionMutation.mutate(
            {id: projectId, projectVersion},
            {
                onSuccess: async () => {
                    await queryClient.invalidateQueries({queryKey: ['automationProjects']});

                    const automationProjectsData = queryClient.getQueryData<AutomationProjectsQuery>([
                        'automationProjects',
                    ]);

                    const restoredProject = automationProjectsData?.automationProjects.find(
                        (automationProject) => automationProject.id === projectId
                    );

                    if (
                        restoredProject &&
                        !restoredProject.workflowTemplates.some(
                            (workflowTemplate) => workflowTemplate.workflowUuid === currentWorkflowUuid
                        )
                    ) {
                        const firstWorkflowUuid = restoredProject.workflowTemplates[0]?.workflowUuid;

                        void navigate(
                            firstWorkflowUuid
                                ? `/embedded/automations/${firstWorkflowUuid}/editor`
                                : '/embedded/automations'
                        );
                    }

                    clearWorkflowHistory();

                    useWorkflowNodeDetailsPanelStore.getState().reset();

                    void queryClient.invalidateQueries({queryKey: ['automationProjectVersions']});
                    void queryClient.invalidateQueries({queryKey: ['automationProjectVersionWorkflowChanges']});
                    void queryClient.invalidateQueries({queryKey: ['automationProjectVersionWorkflowChange']});

                    toast(`Version V${projectVersion} applied to the draft.`);

                    onSuccess();
                },
            }
        );
    };

    return {
        handleRestoreProjectVersion,
        restoreProjectVersionPending: restoreProjectVersionMutation.isPending,
    };
};
