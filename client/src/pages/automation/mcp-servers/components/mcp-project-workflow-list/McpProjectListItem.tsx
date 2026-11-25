import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import {FolderIcon} from 'lucide-react';
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
            <Collapsible className="group" onOpenChange={setExpanded} open={expanded}>
                <div className="relative flex items-center gap-2.5 rounded-md py-2.5 pl-3 hover:bg-surface-neutral-primary-hover">
                    <ListItemExpandButton
                        className="mr-0 after:absolute after:inset-0"
                        label={expanded ? 'Collapse project' : 'Expand project'}
                    />

                    <FolderIcon aria-hidden="true" className="size-6 shrink-0 text-content-neutral-secondary" />

                    <CollapsibleTrigger asChild>
                        <button className="flex min-w-0 cursor-pointer items-center text-left" type="button">
                            <span className="truncate text-sm font-medium">
                                {mcpProject.project?.name || `Project ${mcpProject.projectDeploymentId}`}
                            </span>
                        </button>
                    </CollapsibleTrigger>

                    <div className="min-w-0 flex-1" />

                    {mcpProject.projectVersion && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    className="relative z-10"
                                    label={`v${mcpProject.projectVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Project Version</TooltipContent>
                        </Tooltip>
                    )}

                    <Tooltip>
                        <TooltipTrigger className="relative z-10 flex min-w-56 items-center justify-end text-sm text-content-neutral-secondary tabular-nums">
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
                    <div className="px-3 py-2 pl-10">
                        <McpProjectWorkflowList
                            mcpProject={mcpProject}
                            mcpProjectWorkflows={mcpProject.mcpProjectWorkflows}
                        />
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
