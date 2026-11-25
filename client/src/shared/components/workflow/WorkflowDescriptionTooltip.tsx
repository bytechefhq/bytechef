import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ReactElement} from 'react';

export const WORKFLOW_DESCRIPTION_TOOLTIP_DELAY_DURATION = 500;

interface WorkflowDescriptionTooltipProps {
    children: ReactElement;
    description?: string;
}

const WorkflowDescriptionTooltip = ({children, description}: WorkflowDescriptionTooltipProps) => {
    if (!description?.trim()) {
        return children;
    }

    return (
        <Tooltip delayDuration={WORKFLOW_DESCRIPTION_TOOLTIP_DELAY_DURATION}>
            <TooltipTrigger asChild>{children}</TooltipTrigger>

            <TooltipContent align="start" className="max-w-md break-words whitespace-pre-wrap">
                {description}
            </TooltipContent>
        </Tooltip>
    );
};

export default WorkflowDescriptionTooltip;
