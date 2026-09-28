import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {EditIcon, EllipsisVerticalIcon, SquareArrowOutUpRightIcon} from 'lucide-react';

interface ProjectDeploymentWorkflowListItemDropDownProps {
    onEditClick: () => void;
    onOpenInProjectClick?: () => void;
    workflow: Workflow;
}

const ProjectDeploymentWorkflowListItemDropdownMenu = ({
    onEditClick,
    onOpenInProjectClick,
    workflow,
}: ProjectDeploymentWorkflowListItemDropDownProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="p-0">
                <DropdownMenuItem
                    className="dropdown-menu-item"
                    disabled={workflow.connectionsCount === 0 && workflow?.inputsCount === 0}
                    onClick={onEditClick}
                >
                    <EditIcon /> Edit
                </DropdownMenuItem>

                {onOpenInProjectClick && (
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onOpenInProjectClick}>
                        <SquareArrowOutUpRightIcon /> Open in Project
                    </DropdownMenuItem>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentWorkflowListItemDropdownMenu;
