import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {RocketIcon} from 'lucide-react';

interface ProjectListItemNewDeploymentMenuItemProps {
    hasWorkflows: boolean;
    isPublished: boolean;
    onClick: () => void;
}

const getDisabledReason = (hasWorkflows: boolean, isPublished: boolean) => {
    if (!hasWorkflows) {
        return 'Add a workflow before deploying the project.';
    }

    if (!isPublished) {
        return 'Publish the project before deploying it.';
    }

    return undefined;
};

const ProjectListItemNewDeploymentMenuItem = ({
    hasWorkflows,
    isPublished,
    onClick,
}: ProjectListItemNewDeploymentMenuItemProps) => {
    const disabledReason = getDisabledReason(hasWorkflows, isPublished);

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
