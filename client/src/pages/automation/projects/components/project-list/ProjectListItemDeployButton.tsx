import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {RocketIcon} from 'lucide-react';
import {ComponentPropsWithRef} from 'react';

type ProjectListItemDeployButtonPropsType = Omit<ComponentPropsWithRef<'button'>, 'children'> & {
    hasWorkflows: boolean;
};

const ProjectListItemDeployButton = ({hasWorkflows, ...props}: ProjectListItemDeployButtonPropsType) => {
    const deployButton = (
        <Button
            className="hover:bg-surface-neutral-primary-hover"
            size="sm"
            variant="outline"
            {...props}
            disabled={!hasWorkflows}
        >
            <RocketIcon /> Deploy
        </Button>
    );

    if (hasWorkflows) {
        return deployButton;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className="inline-block" data-interactive>
                    {deployButton}
                </span>
            </TooltipTrigger>

            <TooltipContent>Add a workflow before deploying the project.</TooltipContent>
        </Tooltip>
    );
};

export default ProjectListItemDeployButton;
