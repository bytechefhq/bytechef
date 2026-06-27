import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {ChevronDownIcon, ChevronRightIcon, WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

import McpProjectWorkflowDialog from '../McpProjectWorkflowDialog';
import McpProjectListItemDropdownMenu from './McpProjectListItemDropdownMenu';
import McpProjectWorkflowList from './McpProjectWorkflowList';
import {McpProjectItemType} from './hooks/useMcpProjectList';
import useMcpProjectListItem from './hooks/useMcpProjectListItem';

interface McpProjectListItemProps {
    mcpProject: McpProjectItemType;
}

const McpProjectListItem = ({mcpProject}: McpProjectListItemProps) => {
    const [expanded, setExpanded] = useState(false);

    const {
        handleOnProjectDeploymentDialogClose,
        mcpWorkflowUuids,
        projectDeployment,
        setShowChangeProjectVersionDialog,
        setShowEditWorkflowsDialog,
        showChangeProjectVersionDialog,
        showEditWorkflowsDialog,
    } = useMcpProjectListItem(mcpProject);

    return (
        <>
            <Collapsible className="group rounded-md border border-border" onOpenChange={setExpanded} open={expanded}>
                <div className="flex items-center gap-2.5 px-3 py-2.5">
                    <CollapsibleTrigger asChild>
                        <button
                            aria-label={expanded ? 'Collapse project' : 'Expand project'}
                            className="shrink-0 text-muted-foreground hover:text-foreground"
                            type="button"
                        >
                            {expanded ? (
                                <ChevronDownIcon className="size-4" />
                            ) : (
                                <ChevronRightIcon className="size-4" />
                            )}
                        </button>
                    </CollapsibleTrigger>

                    <WorkflowIcon className="size-6 shrink-0 text-content-neutral-secondary" />

                    <CollapsibleTrigger asChild>
                        <button className="flex min-w-0 flex-1 cursor-pointer items-center text-left" type="button">
                            <span className="truncate text-sm font-medium">
                                {mcpProject.project?.name || `Project ${mcpProject.projectDeploymentId}`}
                            </span>
                        </button>
                    </CollapsibleTrigger>

                    {mcpProject.projectVersion && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`v${mcpProject.projectVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Project Version</TooltipContent>
                        </Tooltip>
                    )}

                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                            {mcpProject.lastModifiedDate ? (
                                <span className="text-xs">
                                    {`Modified at ${new Date(mcpProject.lastModifiedDate).toLocaleDateString()} ${new Date(mcpProject.lastModifiedDate).toLocaleTimeString()}`}
                                </span>
                            ) : (
                                '-'
                            )}
                        </TooltipTrigger>

                        <TooltipContent>Last Updated Date</TooltipContent>
                    </Tooltip>

                    <McpProjectListItemDropdownMenu
                        mcpProject={mcpProject}
                        onChangeProjectVersionClick={() => setShowChangeProjectVersionDialog(true)}
                        onEditWorkflowsClick={() => setShowEditWorkflowsDialog(true)}
                    />
                </div>

                <CollapsibleContent>
                    <div className="border-t border-border px-3 py-2 pl-10">
                        <McpProjectWorkflowList mcpProjectWorkflows={mcpProject.mcpProjectWorkflows} />
                    </div>
                </CollapsibleContent>
            </Collapsible>

            {showEditWorkflowsDialog && (
                <McpProjectWorkflowDialog mcpProject={mcpProject} onClose={() => setShowEditWorkflowsDialog(false)} />
            )}

            {showChangeProjectVersionDialog && (
                <ProjectDeploymentDialog
                    changeProjectVersion={true}
                    filterWorkflowUuids={mcpWorkflowUuids}
                    onClose={handleOnProjectDeploymentDialogClose}
                    projectDeployment={projectDeployment}
                    redirectOnSubmit={false}
                />
            )}
        </>
    );
};

export default McpProjectListItem;
