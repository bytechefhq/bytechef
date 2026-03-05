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
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';

interface WorkflowSelectProps {
    currentWorkflowLabel?: string;
    integrationId: number;
    integrationWorkflowId: number;
    integrationWorkflows: Workflow[];
    onValueChange: (integrationWorkflowId: number) => void;
}

const WorkflowSelect = ({
    currentWorkflowLabel,
    integrationWorkflowId,
    integrationWorkflows,
    onValueChange,
}: WorkflowSelectProps) => (
    <Select
        aria-label={`Select workflow (${currentWorkflowLabel})`}
        defaultValue={integrationWorkflowId.toString()}
        name="integrationWorkflowSelect"
        onValueChange={(value) => onValueChange(+value)}
        value={integrationWorkflowId.toString()}
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

        {integrationWorkflows && (
            <SelectContent>
                <SelectGroup>
                    <SelectLabel>Workflows</SelectLabel>

                    {integrationWorkflows.map((workflow) => (
                        <SelectItem
                            className="[&>span]:truncate"
                            key={workflow.integrationWorkflowId!}
                            title={workflow.label!.length > 32 ? workflow.label! : undefined}
                            value={workflow.integrationWorkflowId!.toString()}
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
