import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Workflow} from '@/shared/middleware/automation/configuration';

const WorkflowSelect = ({
    currentWorkflowLabel,
    onValueChange,
    projectWorkflowId,
    projectWorkflows,
}: {
    currentWorkflowLabel: string;
    onValueChange: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    projectWorkflows: Workflow[];
}) => {
    return (
        <Select
            defaultValue={projectWorkflowId.toString()}
            name="projectWorkflowSelect"
            onValueChange={(value) => onValueChange(+value)}
            value={projectWorkflowId.toString()}
        >
            <Tooltip>
                <TooltipTrigger asChild>
                    <SelectTrigger className="[&>span]:line-clamp-0 w-60 gap-2 border shadow-none hover:bg-surface-neutral-primary-hover [&>span]:truncate [&>svg]:min-w-4">
                        <SelectValue className="font-semibold" placeholder="Select a workflow" />
                    </SelectTrigger>
                </TooltipTrigger>

                <TooltipContent>{currentWorkflowLabel}</TooltipContent>
            </Tooltip>

            {projectWorkflows && (
                <SelectContent>
                    <SelectGroup>
                        <SelectLabel>Workflows</SelectLabel>

                        {projectWorkflows.map((workflow) => (
                            <SelectItem
                                className="w-60 [&>span]:truncate"
                                key={workflow.projectWorkflowId!}
                                title={workflow.label!}
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
};

export default WorkflowSelect;
