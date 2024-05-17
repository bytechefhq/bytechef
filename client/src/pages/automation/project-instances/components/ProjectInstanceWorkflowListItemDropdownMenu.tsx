import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {WorkflowModel} from '@/middleware/automation/configuration';
import {DotsVerticalIcon} from '@radix-ui/react-icons';

interface ProjectInstanceWorkflowListItemDropDownProps {
    onEditClick: () => void;
    onEnableClick: () => void;
    projectInstanceEnabled: boolean;
    projectInstanceWorkflowEnabled: boolean;
    workflow: WorkflowModel;
}

const ProjectInstanceWorkflowListItemDropdownMenu = ({
    onEditClick,
    onEnableClick,
    projectInstanceEnabled,
    projectInstanceWorkflowEnabled,
    workflow,
}: ProjectInstanceWorkflowListItemDropDownProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    disabled={
                        projectInstanceEnabled || (workflow.connectionsCount === 0 && workflow?.inputsCount === 0)
                    }
                    onClick={onEditClick}
                >
                    Edit
                </DropdownMenuItem>

                <DropdownMenuItem disabled={projectInstanceEnabled} onClick={onEnableClick}>
                    {projectInstanceWorkflowEnabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectInstanceWorkflowListItemDropdownMenu;
