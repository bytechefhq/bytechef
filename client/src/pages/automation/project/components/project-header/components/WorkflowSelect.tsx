import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {ChevronDownIcon} from 'lucide-react';

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
    <DropdownMenu>
        <Tooltip>
            <TooltipTrigger asChild>
                <DropdownMenuTrigger
                    aria-label="Workflow select"
                    className="flex max-w-64 items-center gap-1 rounded-md px-1.5 py-1 font-semibold text-content-neutral-primary outline-hidden hover:bg-surface-neutral-primary-hover data-[state=open]:bg-surface-neutral-primary-hover"
                >
                    {currentWorkflowLabel ? (
                        <span className="truncate">{currentWorkflowLabel}</span>
                    ) : (
                        <Skeleton className="h-3 w-44" />
                    )}

                    <ChevronDownIcon className="size-4 shrink-0 text-content-neutral-secondary" />
                </DropdownMenuTrigger>
            </TooltipTrigger>

            {currentWorkflowLabel && currentWorkflowLabel.length > 30 && (
                <TooltipContent>{currentWorkflowLabel}</TooltipContent>
            )}
        </Tooltip>

        {projectWorkflows && (
            <DropdownMenuContent align="start" className="max-w-80 min-w-64">
                <DropdownMenuLabel>Workflows</DropdownMenuLabel>

                <DropdownMenuRadioGroup
                    onValueChange={(value) => onValueChange(+value)}
                    value={projectWorkflowId.toString()}
                >
                    {projectWorkflows.map((workflow) => (
                        <DropdownMenuRadioItem
                            className="cursor-pointer"
                            key={workflow.projectWorkflowId!}
                            title={workflow.label!.length > 32 ? workflow.label! : undefined}
                            value={workflow.projectWorkflowId!.toString()}
                        >
                            <span className="truncate">{workflow.label!}</span>
                        </DropdownMenuRadioItem>
                    ))}
                </DropdownMenuRadioGroup>
            </DropdownMenuContent>
        )}
    </DropdownMenu>
);

export default WorkflowSelect;
