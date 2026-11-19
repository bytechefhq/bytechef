import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import {McpServer} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

import McpAutomationProjectConfigurationDialog from './McpAutomationProjectConfigurationDialog';
import McpAutomationProjectConfigurationListItemDropdownMenu from './McpAutomationProjectConfigurationListItemDropdownMenu';
import McpAutomationProjectConfigurationWorkflowListItem from './McpAutomationProjectConfigurationWorkflowListItem';
import {McpAutomationProjectConfigurationItemType} from './hooks/useMcpAutomationProjectConfigurationList';

interface McpAutomationProjectConfigurationListItemProps {
    mcpAutomationProjectConfiguration: McpAutomationProjectConfigurationItemType;
    mcpServer?: McpServer;
}

const McpAutomationProjectConfigurationListItem = ({
    mcpAutomationProjectConfiguration,
    mcpServer,
}: McpAutomationProjectConfigurationListItemProps) => {
    const [expanded, setExpanded] = useState(false);
    const [showEditWorkflowsDialog, setShowEditWorkflowsDialog] = useState(false);

    return (
        <>
            <Collapsible className="group" onOpenChange={setExpanded} open={expanded}>
                <div className="relative flex items-center gap-2.5 rounded-md py-2.5 pl-3 hover:bg-surface-neutral-primary-hover">
                    <ListItemExpandButton
                        className="mr-0 after:absolute after:inset-0"
                        label={expanded ? 'Collapse automation' : 'Expand automation'}
                    />

                    <WorkflowIcon className="size-6 shrink-0 text-content-neutral-secondary" />

                    <span className="min-w-0 flex-1 truncate text-sm">
                        <span className="font-medium">{mcpAutomationProjectConfiguration.projectName}</span>

                        <span className="ml-2 text-xs text-content-neutral-secondary">
                            {mcpAutomationProjectConfiguration.automationProjectConfigurationName}
                        </span>
                    </span>

                    <Badge
                        className="relative z-10"
                        label={`v${mcpAutomationProjectConfiguration.projectVersion}`}
                        styleType="secondary-filled"
                        weight="semibold"
                    />

                    {!mcpAutomationProjectConfiguration.automationProjectConfigurationEnabled && (
                        <Badge label="Turned off" styleType="secondary-filled" weight="semibold" />
                    )}

                    <McpAutomationProjectConfigurationListItemDropdownMenu
                        mcpAutomationProjectConfiguration={mcpAutomationProjectConfiguration}
                        onEditWorkflowsClick={() => setShowEditWorkflowsDialog(true)}
                    />
                </div>

                <CollapsibleContent>
                    <div className="divide-y divide-stroke-neutral-primary px-3 py-2 pl-10">
                        {mcpAutomationProjectConfiguration.workflows.map((workflow) => (
                            <McpAutomationProjectConfigurationWorkflowListItem
                                environmentId={mcpServer?.environmentId}
                                key={workflow.id}
                                projectName={mcpAutomationProjectConfiguration.projectName}
                                workflow={workflow}
                            />
                        ))}
                    </div>
                </CollapsibleContent>
            </Collapsible>

            {showEditWorkflowsDialog && mcpServer && (
                <McpAutomationProjectConfigurationDialog
                    mcpAutomationProjectConfiguration={mcpAutomationProjectConfiguration}
                    mcpServer={mcpServer}
                    onClose={() => setShowEditWorkflowsDialog(false)}
                />
            )}
        </>
    );
};

export default McpAutomationProjectConfigurationListItem;
