import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useDuplicateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';

const ProjectHeaderWorkflowDropDownMenu = ({
    onShowDeleteWorkflowAlertDialog,
    projectId,
    workflowId,
}: {
    onShowDeleteWorkflowAlertDialog: () => void;
    projectId: number;
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
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <div>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button className="hover:bg-gray-200" size="icon" variant="ghost">
                                <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                            </Button>
                        </TooltipTrigger>

                        <TooltipContent>Workflow Settings</TooltipContent>
                    </Tooltip>
                </div>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    onClick={() => {
                        setShowEditWorkflowDialog(true);
                    }}
                >
                    Edit
                </DropdownMenuItem>

                <DropdownMenuItem
                    onClick={() =>
                        duplicateWorkflowMutation.mutate({
                            id: projectId,
                            workflowId: workflowId,
                        })
                    }
                >
                    Duplicate
                </DropdownMenuItem>

                <DropdownMenuItem
                    onClick={() => (window.location.href = `/api/automation/internal/workflows/${workflowId}/export`)}
                >
                    Export
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-destructive" onClick={() => onShowDeleteWorkflowAlertDialog()}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectHeaderWorkflowDropDownMenu;
