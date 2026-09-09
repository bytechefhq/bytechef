import {DropdownMenuItem} from '@/components/ui/dropdown-menu';
import AutomationWorkflowDialog, {
    AutomationWorkflowFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog';
import WorkflowListItemDropdownMenu from '@/shared/components/workflow/WorkflowListItemDropdownMenu';
import {
    AutomationWorkflowProjectsQuery,
    useDeleteAutomationWorkflowProjectWorkflowMutation,
    useDuplicateAutomationWorkflowProjectWorkflowMutation,
    useUpdateAutomationWorkflowProjectWorkflowMutation,
} from '@/shared/middleware/graphql';

import '@/shared/styles/dropdownMenu.css';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType = AutomationWorkflowProjectType['workflowTemplates'][number];

interface AutomationWorkflowEditorWorkflowsListItemDropdownMenuProps {
    currentWorkflowId: string;
    project: AutomationWorkflowProjectType;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}

const AutomationWorkflowEditorWorkflowsListItemDropdownMenu = ({
    currentWorkflowId,
    project,
    workflow,
}: AutomationWorkflowEditorWorkflowsListItemDropdownMenuProps) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const deleteWorkflowMutation = useDeleteAutomationWorkflowProjectWorkflowMutation();
    const duplicateWorkflowMutation = useDuplicateAutomationWorkflowProjectWorkflowMutation();
    const updateWorkflowMutation = useUpdateAutomationWorkflowProjectWorkflowMutation();

    const invalidateProjects = () => {
        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
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
                        navigate(`/embedded/automation-workflows/${firstRemainingWorkflow.workflowUuid}/editor`);
                    } else {
                        navigate('/embedded/automation-workflows');
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

    const handleEditWorkflowSubmit = (values: AutomationWorkflowFormValuesI) => {
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
                    <AutomationWorkflowDialog
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
            <DropdownMenuItem className="dropdown-menu-item" onClick={handleDuplicateWorkflowClick}>
                <CopyIcon /> Duplicate
            </DropdownMenuItem>
        </WorkflowListItemDropdownMenu>
    );
};

export default AutomationWorkflowEditorWorkflowsListItemDropdownMenu;
