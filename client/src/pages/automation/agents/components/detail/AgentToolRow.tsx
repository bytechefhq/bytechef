import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AgentToolRowLabel from '@/pages/automation/agents/components/detail/AgentToolRowLabel';
import {AiAgentElement} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {SettingsIcon, Trash2Icon, WorkflowIcon} from 'lucide-react';

interface AgentToolRowProps {
    element: AiAgentElement;
    isBusy: boolean;
    onConfigure: (element: AiAgentElement) => void;
    onDelete: (element: AiAgentElement) => void;
    onRequiresApprovalToggle: (element: AiAgentElement, checked: boolean) => void;
}

const AgentToolRow = ({element, isBusy, onConfigure, onDelete, onRequiresApprovalToggle}: AgentToolRowProps) => {
    const actionName = String(element.parameters?.actionName ?? '');
    const componentName = String(element.parameters?.componentName ?? '');
    const componentVersion = Number(element.parameters?.componentVersion ?? 1);

    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {componentName, componentVersion},
        !!componentName
    );

    return (
        <li
            aria-label={`${componentName} / ${actionName}`}
            className="flex items-center justify-between rounded-md border border-border/50 p-3"
        >
            <AgentToolRowLabel
                actionName={actionName}
                componentDefinition={componentDefinition}
                componentName={componentName}
            />

            <div className="flex items-center gap-1">
                <Switch
                    checked={!!element.parameters?.requiresApproval}
                    disabled={isBusy}
                    label="Requires approval"
                    onCheckedChange={(checked) => onRequiresApprovalToggle(element, checked)}
                    variant="small"
                />

                {componentDefinition?.clusterRoot ? (
                    <Tooltip>
                        <TooltipTrigger
                            aria-label="Configure on the workflow canvas"
                            className="flex size-8 items-center justify-center text-muted-foreground"
                        >
                            <WorkflowIcon className="size-4" />
                        </TooltipTrigger>

                        <TooltipContent className="max-w-xs">
                            This tool has its own cluster elements. Configure it in the workflow canvas&apos;s cluster
                            element editor.
                        </TooltipContent>
                    </Tooltip>
                ) : (
                    <Button
                        aria-label="Configure tool"
                        icon={<SettingsIcon />}
                        onClick={() => onConfigure(element)}
                        size="iconSm"
                        variant="ghost"
                    />
                )}

                <Button
                    aria-label="Delete tool"
                    icon={<Trash2Icon />}
                    onClick={() => onDelete(element)}
                    size="iconSm"
                    variant="destructiveGhost"
                />
            </div>
        </li>
    );
};

export default AgentToolRow;
