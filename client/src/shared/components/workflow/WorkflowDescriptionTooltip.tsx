import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/Tooltip/Tooltip';
import {ReactElement} from 'react';

interface WorkflowDescriptionTooltipProps {
    children: ReactElement;
    description?: string;
}

const WorkflowDescriptionTooltip = ({children, description}: WorkflowDescriptionTooltipProps) => {
    if (!description?.trim()) {
        return children;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>{children}</TooltipTrigger>

            <TooltipContent align="start" className="max-w-md break-words whitespace-pre-wrap">
                {description}
            </TooltipContent>
        </Tooltip>
    );
};

export default WorkflowDescriptionTooltip;
