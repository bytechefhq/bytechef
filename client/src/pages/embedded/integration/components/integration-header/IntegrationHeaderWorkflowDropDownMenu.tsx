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
import {EllipsisVerticalIcon} from 'lucide-react';

const IntegrationHeaderWorkflowDropDownMenu = ({
    onShowDeleteWorkflowAlertDialog,
    workflowId,
}: {
    onShowDeleteWorkflowAlertDialog: () => void;
    workflowId: string;
}) => {
    const {setShowEditWorkflowDialog} = useWorkflowEditorStore();

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
                    onClick={() => (window.location.href = `/api/embedded/internal/workflows/${workflowId}/export`)}
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

export default IntegrationHeaderWorkflowDropDownMenu;
