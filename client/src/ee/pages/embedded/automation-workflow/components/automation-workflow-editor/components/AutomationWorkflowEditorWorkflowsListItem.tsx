import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

type AutomationWorkflowProjectWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];

interface AutomationWorkflowEditorWorkflowsListItemProps {
    currentWorkflowId: string;
    onWorkflowClick: (workflowUuid: string) => void;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}

const AutomationWorkflowEditorWorkflowsListItem = ({
    currentWorkflowId,
    onWorkflowClick,
    workflow,
}: AutomationWorkflowEditorWorkflowsListItemProps) => {
    const editedDate = workflow.lastModifiedDate ? new Date(workflow.lastModifiedDate).toLocaleDateString() : undefined;

    return (
        <li
            className={twMerge(
                'w-80 cursor-pointer self-start rounded-md border border-transparent p-3 hover:bg-background',
                workflow.workflowUuid === currentWorkflowId && 'border-stroke-brand-primary bg-background'
            )}
            onClick={() => onWorkflowClick(workflow.workflowUuid)}
        >
            <div className="flex flex-col gap-3 overflow-hidden">
                {(workflow.triggers.length > 0 || workflow.components.length > 0) && (
                    <div className="flex flex-wrap gap-2">
                        {[...workflow.triggers, ...workflow.components].map((item, index) => (
                            <div
                                className="flex shrink-0 items-center justify-center rounded-full border bg-background p-1"
                                key={`${index}-${item.name}`}
                                title={item.title ?? item.name}
                            >
                                {item.icon && <InlineSVG className="size-5 flex-none" src={item.icon} />}
                            </div>
                        ))}
                    </div>
                )}

                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className="flex flex-col gap-1 text-start">
                            <span className="truncate overflow-hidden text-sm font-medium">{workflow.label}</span>

                            {editedDate && (
                                <div className="flex gap-1 text-xs text-content-neutral-secondary">
                                    <span>Edited</span>

                                    <span>{editedDate}</span>
                                </div>
                            )}
                        </div>
                    </TooltipTrigger>

                    {workflow.label && workflow.label.length > 40 && (
                        <TooltipContent className="max-w-96">{workflow.label}</TooltipContent>
                    )}
                </Tooltip>
            </div>
        </li>
    );
};

export default AutomationWorkflowEditorWorkflowsListItem;
