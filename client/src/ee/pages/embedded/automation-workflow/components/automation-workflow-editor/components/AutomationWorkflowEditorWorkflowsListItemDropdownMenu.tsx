import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import AutomationWorkflowDialog, {
    AutomationWorkflowFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog';
import WorkflowListItemDropdownMenu from '@/shared/components/workflow/WorkflowListItemDropdownMenu';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {
    AutomationWorkflowProjectsQuery,
    useDeleteAutomationWorkflowProjectWorkflowMutation,
    useDuplicateAutomationWorkflowProjectWorkflowMutation,
    useUpdateAutomationWorkflowProjectWorkflowMutation,
} from '@/shared/middleware/graphql';
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

    const canCreateWorkflow = useHasEmbeddedScope('WORKFLOW_CREATE', DEVELOPMENT_ENVIRONMENT);
    const canDeleteWorkflow = useHasEmbeddedScope('WORKFLOW_DELETE', DEVELOPMENT_ENVIRONMENT);
    const canEditWorkflow = useHasEmbeddedScope('WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);

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
            showDeleteAction={canDeleteWorkflow}
            showEditAction={canEditWorkflow}
            workflowLabel={workflow.label ?? workflow.workflowUuid}
        >
            {canCreateWorkflow && (
                <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onClick={handleDuplicateWorkflowClick} />
            )}
        </WorkflowListItemDropdownMenu>
    );
};

export default AutomationWorkflowEditorWorkflowsListItemDropdownMenu;
