import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {getPathAfterWorkflowDelete} from '@/ee/pages/embedded/automation/components/automation-editor/utils/getPathAfterWorkflowDelete';
import AutomationDialog, {
    AutomationFormValuesI,
} from '@/ee/pages/embedded/automations/components/automation-dialog/AutomationDialog';
import WorkflowListItemDropdownMenu from '@/shared/components/workflow/WorkflowListItemDropdownMenu';
import {
    AutomationProjectsQuery,
    useDeleteAutomationProjectWorkflowMutation,
    useDuplicateAutomationProjectWorkflowMutation,
    useUpdateAutomationProjectWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon, DownloadIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type AutomationProjectWorkflowTemplateType = AutomationProjectType['workflowTemplates'][number];

interface AutomationEditorWorkflowsListItemDropdownMenuProps {
    currentWorkflowId: string;
    project: AutomationProjectType;
    workflow: AutomationProjectWorkflowTemplateType;
}

const AutomationEditorWorkflowsListItemDropdownMenu = ({
    currentWorkflowId,
    project,
    workflow,
}: AutomationEditorWorkflowsListItemDropdownMenuProps) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const deleteWorkflowMutation = useDeleteAutomationProjectWorkflowMutation();
    const duplicateWorkflowMutation = useDuplicateAutomationProjectWorkflowMutation();
    const updateWorkflowMutation = useUpdateAutomationProjectWorkflowMutation();

    const invalidateProjects = () => {
        void queryClient.invalidateQueries({queryKey: ['automationProjects']});
    };

    const handleDeleteWorkflowClick = () => {
        deleteWorkflowMutation.mutate(
            {workflowUuid: workflow.workflowUuid},
            {
                onSuccess: () => {
                    invalidateProjects();

                    if (workflow.workflowUuid !== currentWorkflowId) {
                        return;
                    }

                    void navigate(getPathAfterWorkflowDelete(project.workflowTemplates, workflow.workflowUuid));
                },
            }
        );
    };

    const handleDuplicateWorkflowClick = () => {
        duplicateWorkflowMutation.mutate(
            {workflowUuid: workflow.workflowUuid},
            {
                onSuccess: () => {
                    invalidateProjects();
                },
            }
        );
    };

    const handleExportWorkflowClick = () => {
        window.location.href = '/api/automation/internal/workflows/' + workflow.workflowId + '/export';
    };

    const handleEditWorkflowSubmit = (values: AutomationFormValuesI) => {
        updateWorkflowMutation.mutate(
            {
                description: values.description || undefined,
                label: values.label,
                workflowUuid: workflow.workflowUuid,
            },
            {
                onSuccess: () => {
                    invalidateProjects();

                    setShowEditWorkflowDialog(false);
                },
            }
        );
    };

    return (
        <WorkflowListItemDropdownMenu
            dialogs={
                showEditWorkflowDialog && (
                    <AutomationDialog
                        onClose={() => setShowEditWorkflowDialog(false)}
                        onSubmit={handleEditWorkflowSubmit}
                        workflow={{description: workflow.description, label: workflow.label}}
                    />
                )
            }
            onDelete={handleDeleteWorkflowClick}
            onEditClick={() => setShowEditWorkflowDialog(true)}
            workflowLabel={workflow.label ?? workflow.workflowUuid}
        >
            <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onClick={handleDuplicateWorkflowClick} />

            <DropdownMenuItem icon={<DownloadIcon />} label="Export" onClick={handleExportWorkflowClick} />
        </WorkflowListItemDropdownMenu>
    );
};

export default AutomationEditorWorkflowsListItemDropdownMenu;
