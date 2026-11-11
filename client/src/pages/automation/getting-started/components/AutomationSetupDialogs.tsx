import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project} from '@/shared/middleware/automation/configuration';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';

import {getGuideWorkflowHref, rememberGuideProject, rememberGuideWorkflow} from '../guideWorkflowStorage';

export const DEFAULT_PROJECT_NAME = 'Getting started';

export const DEFAULT_WORKFLOW_LABEL = 'Learn ByteChef by doing';

interface AutomationSetupDialogsProps {
    onClose: () => void;
    onProjectCreated?: (projectId: number) => void;
    openDialog: 'project' | 'workflow' | null;
    workflowProjectId?: number;
}

const AutomationSetupDialogs = ({
    onClose,
    onProjectCreated,
    openDialog,
    workflowProjectId,
}: AutomationSetupDialogsProps) => {
    const {captureProjectWorkflowCreated} = useAnalytics();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (response) => {
            captureProjectWorkflowCreated();

            if (workflowProjectId !== undefined && response.projectWorkflowId !== undefined) {
                const guideWorkflow = {projectId: workflowProjectId, projectWorkflowId: response.projectWorkflowId};

                rememberGuideWorkflow(guideWorkflow);

                void queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
                void queryClient.invalidateQueries({queryKey: ['automationGettingStarted']});

                void navigate(getGuideWorkflowHref(guideWorkflow));
            }
        },
    });

    return (
        <>
            {openDialog === 'project' && (
                <ProjectDialog
                    onClose={onClose}
                    onSuccess={(projectId) => {
                        if (projectId) {
                            rememberGuideProject(projectId);

                            onProjectCreated?.(projectId);
                        }
                    }}
                    project={{name: DEFAULT_PROJECT_NAME} as Project}
                />
            )}

            {openDialog === 'workflow' && workflowProjectId !== undefined && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    defaultLabel={DEFAULT_WORKFLOW_LABEL}
                    onClose={onClose}
                    parentId={workflowProjectId}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}
        </>
    );
};

export default AutomationSetupDialogs;
