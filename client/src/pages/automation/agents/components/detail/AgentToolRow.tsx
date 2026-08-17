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

/**
 * One attached tool: its label, the requires-approval switch, and the configure and delete actions.
 *
 * Owns the row's component definition query — one cached request per distinct component, shared by every row
 * using it — because both halves of the row read from it: the label needs the titles and icon, and the actions
 * need `clusterRoot`.
 */
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
            // The visible label renders component and action TITLES in separate nodes, so this
            // carries the stable raw identity for assistive tech and tests.
            aria-label={`${componentName} / ${actionName}`}
            className="flex items-center justify-between rounded-md border border-border/50 p-3"
        >
            <AgentToolRowLabel
                actionName={actionName}
                componentDefinition={componentDefinition}
                componentName={componentName}
            />

            <div className="flex items-center gap-2">
                <Switch
                    checked={!!element.parameters?.requiresApproval}
                    disabled={isBusy}
                    label="Requires approval"
                    onCheckedChange={(checked) => onRequiresApprovalToggle(element, checked)}
                    variant="small"
                />

                {componentDefinition?.clusterRoot ? (
                    // A cluster root holds its properties on its child cluster elements, so the config dialog
                    // has nothing to show for one. The picker already keeps cluster roots out, but a row can
                    // still arrive through the agent tool API, the copilot or MCP, and it stays view-only here
                    // rather than offering an action that opens an empty dialog.
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
