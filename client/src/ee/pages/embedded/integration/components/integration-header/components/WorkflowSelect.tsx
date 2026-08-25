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
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {ChevronDownIcon} from 'lucide-react';

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

        {integrationWorkflows && (
            <DropdownMenuContent align="start" className="max-w-80 min-w-64">
                <DropdownMenuLabel>Workflows</DropdownMenuLabel>

                <DropdownMenuRadioGroup
                    onValueChange={(value) => onValueChange(+value)}
                    value={integrationWorkflowId.toString()}
                >
                    {integrationWorkflows.map((workflow) => (
                        <DropdownMenuRadioItem
                            className="cursor-pointer"
                            key={workflow.integrationWorkflowId!}
                            title={workflow.label!.length > 32 ? workflow.label! : undefined}
                            value={workflow.integrationWorkflowId!.toString()}
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
