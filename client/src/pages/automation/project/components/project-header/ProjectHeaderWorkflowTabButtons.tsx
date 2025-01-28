import {Button} from '@/components/ui/button';
import {Separator} from '@/components/ui/separator';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {Project} from '@/shared/middleware/automation/configuration';
import {useDuplicateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon, EditIcon, Trash2Icon, UploadIcon} from 'lucide-react';

const ProjectHeaderWorkflowTabButtons = ({
    onShowDeleteWorkflowAlertDialog,
    project,
    workflowId,
}: {
    onShowDeleteWorkflowAlertDialog: () => void;
    project: Project;
    workflowId: string;
}) => {
    const {setShowEditWorkflowDialog} = useWorkflowEditorStore();

    const queryClient = useQueryClient();

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    return (
        <div className="flex flex-col">
            <Button
                className="justify-start hover:bg-surface-neutral-primary-hover"
                onClick={() => setShowEditWorkflowDialog(true)}
                variant="ghost"
            >
                <EditIcon /> Edit Workflow
            </Button>

            <Button
                className="justify-start hover:bg-surface-neutral-primary-hover"
                onClick={() =>
                    duplicateWorkflowMutation.mutate({
                        id: project.id!,
                        workflowId: workflowId,
                    })
                }
                variant="ghost"
            >
                <CopyIcon /> Duplicate
            </Button>

            <Button
                className="justify-start hover:bg-surface-neutral-primary-hover"
                onClick={() => (window.location.href = `/api/automation/internal/workflows/${workflowId}/export`)}
                variant="ghost"
            >
                <UploadIcon /> Export
            </Button>

            <Separator />

            <Button
                className="justify-start text-destructive hover:bg-surface-error-secondary hover:text-destructive"
                onClick={() => onShowDeleteWorkflowAlertDialog()}
                variant="ghost"
            >
                <Trash2Icon /> Delete
            </Button>
        </div>
    );
};

export default ProjectHeaderWorkflowTabButtons;
