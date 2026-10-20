import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    disabled={workflow.connectionsCount === 0 && workflow?.inputsCount === 0}
                    icon={<EditIcon />}
                    label="Edit"
                    onClick={onEditClick}
                />

                {onOpenInProjectClick && (
                    <DropdownMenuItem
                        icon={<SquareArrowOutUpRightIcon />}
                        label="Open in Project"
                        onClick={onOpenInProjectClick}
                    />
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentWorkflowListItemDropdownMenu;
