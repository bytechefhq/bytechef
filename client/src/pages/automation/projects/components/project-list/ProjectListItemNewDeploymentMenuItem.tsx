import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {RocketIcon} from 'lucide-react';

interface ProjectListItemNewDeploymentMenuItemProps {
    hasWorkflows: boolean;
    isPublished: boolean;
    onClick: () => void;
}

const ProjectListItemNewDeploymentMenuItem = ({
    hasWorkflows,
    isPublished,
    onClick,
}: ProjectListItemNewDeploymentMenuItemProps) => {
    const disabledReason = !hasWorkflows
        ? 'Add a workflow before deploying the project.'
        : !isPublished
          ? 'Publish the project before deploying it.'
          : undefined;

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className="block">
                    <DropdownMenuItem
                        aria-label="New Deployment"
                        disabled={!!disabledReason}
                        icon={<RocketIcon />}
                        label="New Deployment"
                        onSelect={onClick}
                    />
                </span>
            </TooltipTrigger>

            {disabledReason && <TooltipContent>{disabledReason}</TooltipContent>}
        </Tooltip>
    );
};

export default ProjectListItemNewDeploymentMenuItem;
