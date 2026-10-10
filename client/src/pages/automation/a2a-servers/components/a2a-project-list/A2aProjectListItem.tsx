import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {A2aServer, useDeleteA2aProjectMutation} from '@/shared/middleware/graphql';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {
    ChevronDownIcon,
    ChevronRightIcon,
    EditIcon,
    EllipsisVerticalIcon,
    RefreshCwIcon,
    Trash2Icon,
    WorkflowIcon,
} from 'lucide-react';
import {useMemo, useState} from 'react';

import A2aServerWorkflowDialog from '../A2aServerWorkflowDialog';
import A2aProjectWorkflowList from './A2aProjectWorkflowList';
import {A2aProjectItemType} from './hooks/useA2aProjectList';

interface A2aProjectListItemProps {
    a2aProject: A2aProjectItemType;
    a2aServer: A2aServer;
}

const A2aProjectListItem = ({a2aProject, a2aServer}: A2aProjectListItemProps) => {
    const [expanded, setExpanded] = useState(false);
    const [showChangeProjectVersionDialog, setShowChangeProjectVersionDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditSkillsDialog, setShowEditSkillsDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
    });

    const {data: projectDeployment} = useGetProjectDeploymentQuery(
        Number(a2aProject.projectDeploymentId),
        showChangeProjectVersionDialog && a2aProject.projectDeploymentId != null
    );

    const deleteA2aProjectMutation = useDeleteA2aProjectMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['a2aProjectsByServerId']});

            setShowDeleteDialog(false);
        },
    });

    const projectName = useMemo(
        () => projects?.find((project) => Number(project.id) === Number(a2aProject.projectId))?.name,
        [projects, a2aProject.projectId]
    );

    const a2aWorkflowUuids = useMemo(
        () =>
            projectDeployment?.projectDeploymentWorkflows
                ?.filter(
                    (projectDeploymentWorkflow) =>
                        projectDeploymentWorkflow.workflowUuid != null &&
                        projectDeploymentWorkflow.workflowId != null &&
                        a2aProject.workflowIds.includes(projectDeploymentWorkflow.workflowId)
                )
                .map((projectDeploymentWorkflow) => projectDeploymentWorkflow.workflowUuid!) || [],
        [a2aProject.workflowIds, projectDeployment?.projectDeploymentWorkflows]
    );

    const handleProjectDeploymentDialogClose = () => {
        queryClient
            .invalidateQueries({queryKey: ['a2aProjectsByServerId']})
            .then(() => setShowChangeProjectVersionDialog(false));
    };

    return (
        <>
            <Collapsible
                className="group rounded-md border border-border/50"
                onOpenChange={setExpanded}
                open={expanded}
            >
                <div className="relative flex items-center gap-2.5 px-3 py-2.5">
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

                    <span className="min-w-0 flex-1 truncate text-sm font-medium">
                        {projectName || `Project #${a2aProject.projectId}`}
                    </span>

                    <div className="relative z-10 mr-4 flex shrink-0 items-center gap-6">
                        {a2aProject.projectVersion && (
                            <Badge
                                label={`v${a2aProject.projectVersion}`}
                                styleType="secondary-filled"
                                weight="semibold"
                            />
                        )}

                        {a2aProject.lastModifiedDate && (
                            <span className="text-xs text-content-neutral-secondary tabular-nums">
                                {`Modified at ${new Date(a2aProject.lastModifiedDate).toLocaleDateString()} ${new Date(a2aProject.lastModifiedDate).toLocaleTimeString()}`}
                            </span>
                        )}
                    </div>

                    <div className="relative z-10">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="Project actions"
                                    icon={<EllipsisVerticalIcon />}
                                    size="iconSm"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowEditSkillsDialog(true)}
                                >
                                    <EditIcon /> Edit Workflows
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    disabled={a2aProject.projectDeploymentId == null}
                                    onClick={() => setShowChangeProjectVersionDialog(true)}
                                >
                                    <RefreshCwIcon /> Change Project Version
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    disabled={deleteA2aProjectMutation.isPending}
                                    onClick={() => setShowDeleteDialog(true)}
                                    variant="destructive"
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>

                <CollapsibleContent>
                    <div className="border-t border-border/50 px-3 py-2 pl-10">
                        <A2aProjectWorkflowList
                            a2aProject={a2aProject}
                            onEditClick={() => setShowEditSkillsDialog(true)}
                        />
                    </div>
                </CollapsibleContent>
            </Collapsible>

            <A2aServerWorkflowDialog
                a2aProject={a2aProject}
                a2aServer={a2aServer}
                onOpenChange={setShowEditSkillsDialog}
                open={showEditSkillsDialog}
            />

            <AlertDialog
                isPending={deleteA2aProjectMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => deleteA2aProjectMutation.mutate({id: a2aProject.id})}
                open={showDeleteDialog}
            />

            {showChangeProjectVersionDialog && projectDeployment && (
                <ProjectDeploymentDialog
                    changeProjectVersion={true}
                    filterWorkflowUuids={a2aWorkflowUuids}
                    onClose={handleProjectDeploymentDialogClose}
                    projectDeployment={projectDeployment}
                    redirectOnSubmit={false}
                />
            )}
        </>
    );
};

export default A2aProjectListItem;
