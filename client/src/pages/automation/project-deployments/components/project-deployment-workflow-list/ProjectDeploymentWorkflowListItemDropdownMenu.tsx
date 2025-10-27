import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {EditIcon, EllipsisVerticalIcon} from 'lucide-react';

interface ProjectDeploymentWorkflowListItemDropDownProps {
    onEditClick: () => void;
    workflow: Workflow;
}

const ProjectDeploymentWorkflowListItemDropdownMenu = ({
    onEditClick,
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
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentWorkflowListItemDropdownMenu;
