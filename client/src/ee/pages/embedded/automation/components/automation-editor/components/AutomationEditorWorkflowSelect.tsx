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
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {ChevronDownIcon} from 'lucide-react';

type AutomationProjectWorkflowTemplateType =
    AutomationProjectsQuery['automationProjects'][number]['workflowTemplates'][number];

interface AutomationEditorWorkflowSelectProps {
    currentWorkflowId: string;
    onValueChange: (workflowUuid: string) => void;
    workflows: AutomationProjectWorkflowTemplateType[];
}

const getWorkflowLabel = (workflow: AutomationProjectWorkflowTemplateType) => workflow.label || workflow.workflowUuid;

const AutomationEditorWorkflowSelect = ({
    currentWorkflowId,
    onValueChange,
    workflows,
}: AutomationEditorWorkflowSelectProps) => {
    const currentWorkflow = workflows.find((workflow) => workflow.workflowUuid === currentWorkflowId);

    const currentWorkflowLabel = currentWorkflow ? getWorkflowLabel(currentWorkflow) : undefined;

    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger asChild>
                    <DropdownMenuTrigger
                        aria-label="Select workflow"
                        className="flex max-w-64 items-center gap-1 rounded-md px-1.5 py-1 text-content-neutral-primary outline-hidden hover:bg-surface-neutral-primary-hover data-[state=open]:bg-surface-neutral-primary-hover"
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

            <DropdownMenuContent align="start" className="w-80">
                <DropdownMenuRadioGroup onValueChange={onValueChange} value={currentWorkflowId}>
                    {workflows.length > 0 && (
                        <>
                            <DropdownMenuLabel>Workflows</DropdownMenuLabel>

                            {workflows.map((workflow) => {
                                const workflowLabel = getWorkflowLabel(workflow);

                                return (
                                    <DropdownMenuRadioItem
                                        className="cursor-pointer pl-2 data-[state=checked]:bg-surface-brand-secondary data-[state=checked]:text-content-brand-primary [&>span:first-child]:hidden"
                                        key={workflow.workflowUuid}
                                        title={workflowLabel.length > 30 ? workflowLabel : undefined}
                                        value={workflow.workflowUuid}
                                    >
                                        <span className="truncate">{workflowLabel}</span>
                                    </DropdownMenuRadioItem>
                                );
                            })}
                        </>
                    )}
                </DropdownMenuRadioGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default AutomationEditorWorkflowSelect;
