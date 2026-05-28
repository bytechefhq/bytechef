import {Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';

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
}: AutomationWorkflowEditorWorkflowSelectProps) => (
    <Select onValueChange={onValueChange} value={currentWorkflowId}>
        <SelectTrigger
            aria-label="Select workflow"
            className="border-stroke-neutral-secondary bg-background px-3 py-2 shadow-none hover:bg-surface-neutral-primary-hover [&>span]:line-clamp-0 [&>span]:truncate [&>svg]:min-w-4"
        >
            <SelectValue placeholder="Select a workflow" />
        </SelectTrigger>

        <SelectContent>
            <SelectGroup>
                {workflows.map((workflow) => (
                    <SelectItem
                        className="cursor-pointer rounded-none hover:bg-surface-neutral-primary-hover [&>span]:truncate"
                        key={workflow.workflowUuid}
                        value={workflow.workflowUuid}
                    >
                        {workflow.label || workflow.workflowUuid}
                    </SelectItem>
                ))}
            </SelectGroup>
        </SelectContent>
    </Select>
);

export default AutomationWorkflowEditorWorkflowSelect;
