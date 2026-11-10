import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ChevronsDownUpIcon, ChevronsUpDownIcon} from 'lucide-react';

interface WorkflowExecutionExpandToggleButtonProps {
    className?: string;
    expanded: boolean;
    onToggle: () => void;
}

const WorkflowExecutionExpandToggleButton = ({
    className,
    expanded,
    onToggle,
}: WorkflowExecutionExpandToggleButtonProps) => {
    const label = expanded ? 'Collapse all' : 'Expand all';

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label={label}
                    className={className}
                    icon={expanded ? <ChevronsDownUpIcon /> : <ChevronsUpDownIcon />}
                    onClick={onToggle}
                    size="iconXs"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
};

export default WorkflowExecutionExpandToggleButton;
