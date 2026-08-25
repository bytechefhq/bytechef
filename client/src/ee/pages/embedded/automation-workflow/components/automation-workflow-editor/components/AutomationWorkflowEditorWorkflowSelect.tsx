import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {ChevronDownIcon} from 'lucide-react';

type AutomationWorkflowProjectWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];

interface AutomationWorkflowEditorWorkflowSelectProps {
    currentWorkflowId: string;
    onValueChange: (workflowUuid: string) => void;
    workflows: AutomationWorkflowProjectWorkflowTemplateType[];
}

const AutomationWorkflowEditorWorkflowSelect = ({
    currentWorkflowId,
    onValueChange,
    workflows,
}: AutomationWorkflowEditorWorkflowSelectProps) => {
    const currentWorkflow = workflows.find((workflow) => workflow.workflowUuid === currentWorkflowId);

    const currentWorkflowLabel = currentWorkflow?.label || currentWorkflow?.workflowUuid;

    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger asChild>
                    <DropdownMenuTrigger
                        aria-label="Select workflow"
                        className="flex max-w-64 items-center gap-1 rounded-md px-1.5 py-1 font-semibold text-content-neutral-primary outline-hidden hover:bg-surface-neutral-primary-hover data-[state=open]:bg-surface-neutral-primary-hover"
                    >
                        <span className="truncate">{currentWorkflowLabel || 'Select a workflow'}</span>

                        <ChevronDownIcon className="size-4 shrink-0 text-content-neutral-secondary" />
                    </DropdownMenuTrigger>
                </TooltipTrigger>

                {currentWorkflowLabel && currentWorkflowLabel.length > 30 && (
                    <TooltipContent>{currentWorkflowLabel}</TooltipContent>
                )}
            </Tooltip>

            <DropdownMenuContent align="start" className="max-w-80 min-w-64">
                <DropdownMenuRadioGroup onValueChange={onValueChange} value={currentWorkflowId}>
                    {workflows.map((workflow) => (
                        <DropdownMenuRadioItem
                            className="cursor-pointer"
                            key={workflow.workflowUuid}
                            value={workflow.workflowUuid}
                        >
                            <span className="truncate">{workflow.label || workflow.workflowUuid}</span>
                        </DropdownMenuRadioItem>
                    ))}
                </DropdownMenuRadioGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default AutomationWorkflowEditorWorkflowSelect;
