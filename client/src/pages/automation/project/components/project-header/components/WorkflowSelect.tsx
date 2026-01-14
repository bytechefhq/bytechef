import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Workflow} from '@/shared/middleware/automation/configuration';

interface WorkflowSelectProps {
    currentWorkflowLabel?: string;
    onValueChange: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    projectWorkflows: Workflow[];
}

const WorkflowSelect = ({
    currentWorkflowLabel,
    onValueChange,
    projectWorkflowId,
    projectWorkflows,
}: WorkflowSelectProps) => (
    <Select
        aria-label={`Select workflow (${currentWorkflowLabel})`}
        defaultValue={projectWorkflowId.toString()}
        name="projectWorkflowSelect"
        onValueChange={(value) => onValueChange(+value)}
        value={projectWorkflowId.toString()}
    >
        <Tooltip>
            <TooltipTrigger asChild>
                <SelectTrigger
                    aria-label="Workflow select"
                    className="[&>span]:line-clamp-0 w-64 gap-2 border shadow-none hover:bg-surface-neutral-primary-hover [&>span]:truncate [&>svg]:min-w-4"
                >
                    <SelectValue className="font-semibold" placeholder="Select a workflow">
                        {!currentWorkflowLabel ? <Skeleton className="h-3 w-44" /> : currentWorkflowLabel}
                    </SelectValue>
                </SelectTrigger>
            </TooltipTrigger>

            {currentWorkflowLabel && currentWorkflowLabel.length > 30 && (
                <TooltipContent>{currentWorkflowLabel}</TooltipContent>
            )}
        </Tooltip>

        {projectWorkflows && (
            <SelectContent>
                <SelectGroup>
                    <SelectLabel>Workflows</SelectLabel>

                    {projectWorkflows.map((workflow) => (
                        <SelectItem
                            className="w-60 [&>span]:truncate"
                            key={workflow.projectWorkflowId!}
                            title={workflow.label!.length > 32 ? workflow.label! : undefined}
                            value={workflow.projectWorkflowId!.toString()}
                        >
                            {workflow.label!}
                        </SelectItem>
                    ))}
                </SelectGroup>
            </SelectContent>
        )}
    </Select>
);

export default WorkflowSelect;
