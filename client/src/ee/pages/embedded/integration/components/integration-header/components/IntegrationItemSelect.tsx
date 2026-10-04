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
import {ChevronDownIcon, WorkflowIcon} from 'lucide-react';

interface IntegrationItemSelectProps {
    currentIntegrationWorkflowId?: number;
    currentLabel?: string;
    integrationWorkflows: Workflow[];
    onWorkflowValueChange: (integrationWorkflowId: number) => void;
}

const IntegrationItemSelect = ({
    currentIntegrationWorkflowId,
    currentLabel,
    integrationWorkflows,
    onWorkflowValueChange,
}: IntegrationItemSelectProps) => (
    <DropdownMenu>
        <Tooltip>
            <TooltipTrigger asChild>
                <DropdownMenuTrigger
                    aria-label="Integration item select"
                    className="flex max-w-64 items-center gap-1 rounded-md px-1.5 py-1 font-semibold text-content-neutral-primary outline-hidden hover:bg-surface-neutral-primary-hover data-[state=open]:bg-surface-neutral-primary-hover"
                >
                    {currentLabel ? (
                        <span className="truncate">{currentLabel}</span>
                    ) : (
                        <Skeleton className="h-3 w-44" />
                    )}

                    <ChevronDownIcon className="size-4 shrink-0 text-content-neutral-secondary" />
                </DropdownMenuTrigger>
            </TooltipTrigger>

            {currentLabel && currentLabel.length > 30 && <TooltipContent>{currentLabel}</TooltipContent>}
        </Tooltip>

        <DropdownMenuContent align="start" className="max-w-lg min-w-64">
            <DropdownMenuRadioGroup
                onValueChange={(value) => onWorkflowValueChange(Number(value))}
                value={currentIntegrationWorkflowId !== undefined ? currentIntegrationWorkflowId.toString() : ''}
            >
                {integrationWorkflows.length > 0 && (
                    <>
                        <DropdownMenuLabel>Workflows</DropdownMenuLabel>

                        {integrationWorkflows.map((workflow) => (
                            <DropdownMenuRadioItem
                                className="cursor-pointer pl-2 data-[state=checked]:bg-surface-brand-secondary data-[state=checked]:text-content-brand-primary [&>span:first-child]:hidden"
                                key={workflow.integrationWorkflowId!}
                                title={workflow.label!.length > 55 ? workflow.label! : undefined}
                                value={workflow.integrationWorkflowId!.toString()}
                            >
                                <WorkflowIcon className="size-4 shrink-0" />

                                <span className="truncate">{workflow.label!}</span>
                            </DropdownMenuRadioItem>
                        ))}
                    </>
                )}
            </DropdownMenuRadioGroup>
        </DropdownMenuContent>
    </DropdownMenu>
);

export default IntegrationItemSelect;
