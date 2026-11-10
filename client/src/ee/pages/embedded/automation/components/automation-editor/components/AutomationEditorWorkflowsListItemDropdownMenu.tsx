import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
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
import {CopyIcon} from 'lucide-react';
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

                    const firstRemainingWorkflow = project.workflowTemplates.find(
                        (workflowTemplate) => workflowTemplate.workflowUuid !== workflow.workflowUuid
                    );

                    if (firstRemainingWorkflow) {
                        void navigate(`/embedded/automations/${firstRemainingWorkflow.workflowUuid}/editor`);
                    } else {
                        void navigate('/embedded/automations');
                    }
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
        </WorkflowListItemDropdownMenu>
    );
};

export default AutomationEditorWorkflowsListItemDropdownMenu;
