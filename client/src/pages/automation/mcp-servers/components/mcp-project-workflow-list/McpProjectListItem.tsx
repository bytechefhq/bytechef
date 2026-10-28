import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import useOpenInProject from '@/pages/automation/project-deployments/hooks/useOpenInProject';
import {ChevronDownIcon, ChevronRightIcon, SquareArrowOutUpRightIcon, WorkflowIcon} from 'lucide-react';
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

    const {openProject} = useOpenInProject();

    const projectId = mcpProject.project?.id != null ? Number(mcpProject.project.id) : undefined;

    const handleOpenProjectClick = () => {
        if (projectId != null) {
            void openProject(projectId);
        }
    };

    return (
        <>
            <Collapsible
                className="group rounded-md border border-border/50"
                onOpenChange={setExpanded}
                open={expanded}
            >
                <div className="relative flex items-center gap-2.5 rounded-md px-3 py-2.5 hover:bg-surface-neutral-primary-hover">
                    <CollapsibleTrigger asChild>
                        <button
                            aria-label={expanded ? 'Collapse project' : 'Expand project'}
                            className="shrink-0 text-muted-foreground after:absolute after:inset-0 hover:text-foreground"
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
                        <button className="flex min-w-0 cursor-pointer items-center text-left" type="button">
                            <span className="truncate text-sm font-medium">
                                {mcpProject.project?.name || `Project ${mcpProject.projectDeploymentId}`}
                            </span>
                        </button>
                    </CollapsibleTrigger>

                    {projectId != null && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Open project"
                                    className="relative z-10 size-6 shrink-0"
                                    icon={<SquareArrowOutUpRightIcon className="size-4" />}
                                    onClick={handleOpenProjectClick}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Open project</TooltipContent>
                        </Tooltip>
                    )}

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
                    <div className="border-t border-border/50 px-3 py-2 pl-10">
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
