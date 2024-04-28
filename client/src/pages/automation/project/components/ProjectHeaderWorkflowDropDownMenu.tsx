import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useDuplicateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';

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
                                <DotsVerticalIcon />
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
                    onClick={() => (window.location.href = `/api/automation/workflows/${workflowId}/export`)}
                >
                    Export
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-red-600" onClick={() => onShowDeleteWorkflowAlertDialog()}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectHeaderWorkflowDropDownMenu;
