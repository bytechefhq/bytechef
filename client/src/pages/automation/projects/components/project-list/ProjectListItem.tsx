import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {ButtonGroup} from '@/components/ui/button-group';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import useOpenSavedProjectDeployment from '@/pages/automation/project-deployments/hooks/useOpenSavedProjectDeployment';
import ProjectDeploymentsSheet, {
    ProjectDeploymentsSheetStateI,
} from '@/pages/automation/project/components/ProjectDeploymentsSheet';
import {ProjectShareDialog} from '@/pages/automation/project/components/ProjectShareDialog';
import ProjectDeploymentsButton from '@/pages/automation/project/components/project-header/components/ProjectDeploymentsButton';
import {useConvertN8nToWorkflow} from '@/pages/automation/project/hooks/useConverterN8nToWorkflow';
import handleImportN8nWorkflow from '@/pages/automation/project/utils/handleImportN8nWorkflow';
import handleImportWorkflow from '@/pages/automation/project/utils/handleImportWorkflow';
import ProjectPublishDialog from '@/pages/automation/projects/components/ProjectPublishDialog';
import ProjectListItemDeployButton from '@/pages/automation/projects/components/project-list/ProjectListItemDeployButton';
import ProjectListItemPublishMenuItem from '@/pages/automation/projects/components/project-list/ProjectListItemPublishMenuItem';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import ResourceVisibilityPicker from '@/shared/components/visibility/ResourceVisibilityPicker';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import EEVersion from '@/shared/edition/EEVersion';
import {ProjectGitConfigurationI, getProjectGitApi} from '@/shared/edition/project-git/projectGitApi';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHasEnabledAiProvider} from '@/shared/hooks/useHasEnabledAiProvider';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useProjectVisibility} from '@/shared/hooks/useProjectVisibility';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {useUpdateProjectTagsMutation} from '@/shared/mutations/automation/projectTags.mutations';
import {useDeleteProjectMutation, useDuplicateProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectCategoryKeys} from '@/shared/queries/automation/projectCategories.queries';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {ProjectTagKeys} from '@/shared/queries/automation/projectTags.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ChevronDownIcon,
    CopyIcon,
    DownloadIcon,
    EditIcon,
    EllipsisVerticalIcon,
    GitBranchIcon,
    GitPullRequestArrowIcon,
    LayoutTemplateIcon,
    LoaderCircleIcon,
    PlusIcon,
    Share2Icon,
    Trash2Icon,
    UploadIcon,
    WorkflowIcon,
} from 'lucide-react';
import {MouseEvent, Suspense, lazy, useCallback, useRef, useState} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

import TagList from '../../../../../shared/components/TagList';
import ProjectDialog from '../ProjectDialog';

interface ProjectItemProps {
    project: Project;
    projectGitConfiguration?: ProjectGitConfigurationI;
    remainingTags?: Tag[];
}

const ProjectGitConfigurationDialog = lazy(
    () => import('@/ee/pages/automation/project/components/ProjectGitConfigurationDialog')
);

const ProjectListItem = ({project, projectGitConfiguration, remainingTags}: ProjectItemProps) => {
    const [projectDeploymentsSheetState, setProjectDeploymentsSheetState] = useState<
        ProjectDeploymentsSheetStateI | undefined
    >();
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showProjectGitConfigurationDialog, setShowProjectGitConfigurationDialog] = useState(false);
    const [showProjectShareDialog, setShowProjectShareDialog] = useState(false);
    const [showPublishProjectDialog, setShowPublishProjectDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);
    const converterHiddenFileInputRef = useRef<HTMLInputElement>(null);
    const workflowsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const {captureProjectWorkflowCreated, captureProjectWorkflowImported} = useAnalytics();
    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.projects);

    const navigate = useNavigate();

    const openSavedProjectDeployment = useOpenSavedProjectDeployment({
        onDevelopmentDeploymentSaved: (projectDeploymentId) =>
            setProjectDeploymentsSheetState({expandedProjectDeploymentId: projectDeploymentId}),
        projectId: project.id,
    });
    const [searchParams] = useSearchParams();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canConfigureProjectGit = useHasWorkspaceScope(currentWorkspaceId, 'WORKSPACE_MANAGE');
    const canCreateDeployment = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_CREATE');
    const canCreateProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_CREATE');
    const canCreateWorkflow = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_CREATE');
    const canDeleteProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_DELETE');
    const canEditProject = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_EDIT');
    const canManageProjectSettings = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_SETTINGS');
    const canPublish = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_PUBLISH');
    const canPullProjectFromGit = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_PULL');

    const canDeployProject = canCreateDeployment && canEditProject;
    const canPublishProject = canPublish && canEditProject;

    const projectVisibility = useProjectVisibility({
        createdBy: project.createdBy,
        projectId: project.id,
        visibility: project.visibility,
    });

    const projectDeploymentsQuery = useGetWorkspaceProjectDeploymentsQuery(
        {
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId ?? 0,
            projectId: project.id ?? 0,
        },
        false
    );

    const ff_1039 = useFeatureFlagsStore()('ff-1039');

    const queryClient = useQueryClient();

    const {convertN8nWorkflow} = useConvertN8nToWorkflow();
    const {hasEnabledAiProvider, isPending: isAiProviderCheckPending} = useHasEnabledAiProvider();

    const importN8nWorkflowDisabled = !isAiProviderCheckPending && !hasEnabledAiProvider;
    const hasWorkflows = (project.projectWorkflowIds?.length ?? 0) > 0;
    const [isImportingN8nWorkflow, setIsImportingN8nWorkflow] = useState(false);

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (response) => {
            captureProjectWorkflowCreated();

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            navigate(`/automation/projects/${project.id}/project-workflows/${response.projectWorkflowId}`);
        },
    });

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: (_, projectId) => {
            queryClient.cancelQueries({queryKey: ProjectKeys.project(projectId)});
            queryClient.removeQueries({queryKey: ProjectKeys.project(projectId)});

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories(currentWorkspaceId!),
            });
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });

            setShowDeleteDialog(false);
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast('Project duplicated successfully.');
        },
    });

    const importProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: () => {
            captureProjectWorkflowImported();

            queryClient.invalidateQueries({queryKey: ProjectKeys.project(project.id!)});
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast('Workflow is imported.');

            if (workflowsCollapsibleTriggerRef.current?.getAttribute('data-state') === 'closed') {
                workflowsCollapsibleTriggerRef.current.click();
            }
        },
    });

    const pullProjectFromGitMutation = getProjectGitApi().usePullProjectFromGitMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast('Project pulled from git repository successfully.');
        },
    });

    const updateProjectGitConfigurationMutation = getProjectGitApi().useUpdateProjectGitConfigurationMutation({});

    const updateProjectTagsMutation = useUpdateProjectTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    const handleUpdateProjectGitConfigurationSubmit = ({
        onSuccess,
        projectGitConfiguration,
    }: {
        projectGitConfiguration: {branch: string; enabled: boolean};
        onSuccess: () => void;
    }) => {
        updateProjectGitConfigurationMutation.mutate(
            {
                id: project.id!,
                projectGitConfiguration,
            },
            {
                onSuccess,
            }
        );
    };

    const handlePullProjectFromGitClick = () => {
        pullProjectFromGitMutation.mutate({id: project.id!});
    };

    const [isProjectDeploymentDialogOpen, setIsProjectDeploymentDialogOpen] = useState(false);

    const handleProjectDeploymentDialogOpen = async (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        if (!currentWorkspaceId || !project.id) {
            return;
        }

        await projectDeploymentsQuery.refetch();
    };

    const handleProjectListItemClick = useCallback(
        (event: MouseEvent) => {
            if (isProjectDeploymentDialogOpen) {
                return;
            }

            const target = event.target as HTMLElement;

            const interactiveSelectors = [
                '[data-interactive]',
                '[role="menu"]',
                '[data-radix-dropdown-menu-item]',
                '[data-radix-dropdown-menu-trigger]',
                '[data-radix-collapsible-trigger]',
            ].join(', ');

            if (target.closest(interactiveSelectors) || workflowsCollapsibleTriggerRef.current?.contains(target)) {
                return;
            }

            workflowsCollapsibleTriggerRef.current?.click();
        },
        [isProjectDeploymentDialogOpen]
    );

    const renderVisibilityPicker = () => {
        if (!projectVisibility.canManage) {
            return null;
        }

        return (
            <div className="min-w-64 p-3">
                <ResourceVisibilityPicker
                    grantedUserIds={projectVisibility.grantedUserIds}
                    onGrantedUserIdsChange={projectVisibility.onGrantedUserIdsChange}
                    onVisibilityChange={projectVisibility.onVisibilityChange}
                    visibility={project.visibility || 'WORKSPACE'}
                    workspaceMembers={projectVisibility.workspaceMembers}
                />
            </div>
        );
    };

    return (
        <>
            <div
                aria-label={`${project.name}_container`}
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-destructive-foreground"
                onClick={(event) => handleProjectListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-3 group-data-[state='open']:border-none">
                    <div
                        aria-label={project.id?.toString() ?? project.name}
                        className="flex-1"
                        data-testid="project-item"
                    >
                        <div className="flex items-center gap-2">
                            {project.projectWorkflowIds && project.projectWorkflowIds.length > 0 ? (
                                <Link
                                    onClick={(event) => event.stopPropagation()}
                                    to={`/automation/projects/${project?.id}/project-workflows/${project?.projectWorkflowIds![0]}?${searchParams}`}
                                >
                                    {project.description ? (
                                        <Tooltip>
                                            <TooltipTrigger>
                                                <span className="text-base font-semibold">{project.name}</span>
                                            </TooltipTrigger>

                                            <TooltipContent>{project.description}</TooltipContent>
                                        </Tooltip>
                                    ) : (
                                        <span className="text-base font-semibold">{project.name}</span>
                                    )}
                                </Link>
                            ) : project.description ? (
                                <Tooltip>
                                    <TooltipTrigger>
                                        <span className="text-base font-semibold">{project.name}</span>
                                    </TooltipTrigger>

                                    <TooltipContent>{project.description}</TooltipContent>
                                </Tooltip>
                            ) : (
                                <CollapsibleTrigger className="text-base font-semibold">
                                    {project.name}
                                </CollapsibleTrigger>
                            )}

                            {projectVisibility.enabled && !projectVisibility.canManage && (
                                <ResourceVisibilityBadge visibility={project.visibility || 'WORKSPACE'} />
                            )}

                            {projectVisibility.canManage && (
                                <div onClick={(event) => event.stopPropagation()}>
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <button
                                                aria-label="Change visibility"
                                                className="cursor-pointer rounded-sm hover:bg-surface-neutral-primary-hover"
                                                type="button"
                                            >
                                                <ResourceVisibilityBadge
                                                    grantedUserCount={projectVisibility.grantedUserIds.length}
                                                    visibility={project.visibility || 'WORKSPACE'}
                                                />
                                            </button>
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="start" className="p-0">
                                            {renderVisibilityPicker()}
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </div>
                            )}
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center gap-2">
                                <CollapsibleTrigger
                                    className="group flex min-w-28 items-center text-xs font-semibold text-muted-foreground"
                                    ref={workflowsCollapsibleTriggerRef}
                                >
                                    <div className="mr-1">
                                        {project.projectWorkflowIds?.length === 1
                                            ? `${project.projectWorkflowIds?.length} workflow`
                                            : `${project.projectWorkflowIds?.length} workflows`}
                                    </div>

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                {canCreateWorkflow && (
                                    <ButtonGroup aria-label="Workflow Creation Actions">
                                        <Button
                                            aria-label="Create Workflow"
                                            onClick={(event) => {
                                                event.stopPropagation();

                                                setShowWorkflowDialog(true);
                                            }}
                                            size="xs"
                                            variant="outline"
                                        >
                                            <PlusIcon />
                                            Workflow
                                        </Button>

                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button
                                                    aria-label="More Workflow Creation Actions"
                                                    icon={
                                                        isImportingN8nWorkflow ? (
                                                            <LoaderCircleIcon className="animate-spin text-primary" />
                                                        ) : (
                                                            <ChevronDownIcon />
                                                        )
                                                    }
                                                    size="xs"
                                                    variant="outline"
                                                >
                                                    <> </>
                                                </Button>
                                            </DropdownMenuTrigger>

                                            <DropdownMenuContent align="end">
                                                <DropdownMenuItem
                                                    aria-label="Create Workflow from Template"
                                                    icon={<LayoutTemplateIcon />}
                                                    label="From Template"
                                                    onClick={(event) => {
                                                        event.stopPropagation();

                                                        navigate(`./${project.id}/templates`);
                                                    }}
                                                />

                                                <DropdownMenuItem
                                                    aria-label="Import Workflow"
                                                    icon={<UploadIcon />}
                                                    label="Import Workflow"
                                                    onClick={(event) => {
                                                        event.stopPropagation();

                                                        if (hiddenFileInputRef.current) {
                                                            hiddenFileInputRef.current.click();
                                                        }
                                                    }}
                                                />

                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <span className="block">
                                                            <DropdownMenuItem
                                                                aria-label="Import n8n Workflow"
                                                                disabled={importN8nWorkflowDisabled}
                                                                icon={<UploadIcon />}
                                                                label="Import n8n Workflow"
                                                                onClick={() => {
                                                                    if (converterHiddenFileInputRef.current) {
                                                                        converterHiddenFileInputRef.current.click();
                                                                    }
                                                                }}
                                                            />
                                                        </span>
                                                    </TooltipTrigger>

                                                    {importN8nWorkflowDisabled && (
                                                        <TooltipContent>
                                                            Enable an AI provider to import n8n workflows.
                                                        </TooltipContent>
                                                    )}
                                                </Tooltip>
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </ButtonGroup>
                                )}

                                <div onClick={(event) => event.stopPropagation()}>
                                    {project.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={project.id!}
                                            readOnly={!canEditProject}
                                            remainingTags={remainingTags}
                                            tags={project.tags}
                                            updateTagsMutation={updateProjectTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex flex-col items-end gap-y-4">
                            <div className="flex items-center space-x-2">
                                {project.lastPublishedDate && project.lastProjectVersion ? (
                                    <>
                                        <Badge className="flex space-x-1" styleType="success-outline" weight="semibold">
                                            <span>V{project.lastProjectVersion - 1}</span>

                                            <span>PUBLISHED</span>
                                        </Badge>

                                        <ButtonGroup>
                                            {canDeployProject && (
                                                <ProjectDeploymentDialog
                                                    environmentEditable={true}
                                                    onOpenChange={setIsProjectDeploymentDialogOpen}
                                                    onSuccess={openSavedProjectDeployment}
                                                    projectDeployment={{
                                                        name: project.name,
                                                        projectId: project.id,
                                                    }}
                                                    projectDeployments={projectDeploymentsQuery.data}
                                                    projectDeploymentsLoading={projectDeploymentsQuery.isFetching}
                                                    redirectOnSubmit={false}
                                                    showTabs
                                                    triggerNode={
                                                        <ProjectListItemDeployButton
                                                            hasWorkflows={hasWorkflows}
                                                            onClick={handleProjectDeploymentDialogOpen}
                                                        />
                                                    }
                                                />
                                            )}

                                            <ProjectDeploymentsButton
                                                onClick={(event) => {
                                                    event.stopPropagation();

                                                    setProjectDeploymentsSheetState({});
                                                }}
                                                size="iconSm"
                                            />
                                        </ButtonGroup>
                                    </>
                                ) : (
                                    <Badge className="flex space-x-1" styleType="secondary-filled" weight="semibold">
                                        <span>V{project.lastProjectVersion}</span>

                                        <span>{project.lastStatus}</span>
                                    </Badge>
                                )}
                            </div>

                            <Tooltip>
                                <TooltipTrigger>
                                    <div className="flex items-center text-sm text-muted-foreground sm:mt-0">
                                        {project.lastPublishedDate ? (
                                            <span className="text-xs">
                                                {`Published at ${project.lastPublishedDate?.toLocaleDateString()} ${project.lastPublishedDate?.toLocaleTimeString()}`}
                                            </span>
                                        ) : (
                                            <span className="text-xs">Not yet published</span>
                                        )}
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent>Last Modified Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="More Project Actions"
                                    data-testid={`${project.id}-moreProjectActionsButton`}
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                {canPublishProject && (
                                    <>
                                        <ProjectListItemPublishMenuItem
                                            hasWorkflows={hasWorkflows}
                                            onClick={() => setShowPublishProjectDialog(true)}
                                        />

                                        <DropdownMenuSeparator />
                                    </>
                                )}

                                {canEditProject && (
                                    <DropdownMenuItem
                                        aria-label="Edit Project"
                                        icon={<EditIcon />}
                                        label="Edit"
                                        onClick={() => setShowEditDialog(true)}
                                    />
                                )}

                                {canCreateProject && (
                                    <DropdownMenuItem
                                        aria-label="Duplicate Project"
                                        icon={<CopyIcon />}
                                        label="Duplicate"
                                        onClick={() => duplicateProjectMutation.mutate(project.id!)}
                                    />
                                )}

                                {project.projectWorkflowIds && project.projectWorkflowIds?.length > 0 && (
                                    <DropdownMenuItem
                                        aria-label="View Workflows"
                                        icon={<WorkflowIcon />}
                                        label="View Workflows"
                                        onClick={() =>
                                            navigate(
                                                `/automation/projects/${project?.id}/project-workflows/${project?.projectWorkflowIds![0]}`
                                            )
                                        }
                                    />
                                )}

                                {canManageProjectSettings && (
                                    <DropdownMenuItem
                                        aria-label="Share Project"
                                        icon={<Share2Icon />}
                                        label="Share"
                                        onClick={() => setShowProjectShareDialog(true)}
                                    />
                                )}

                                {templatesSubmissionForm && (
                                    <DropdownMenuItem
                                        aria-label="Share with Community"
                                        icon={<Share2Icon />}
                                        label="Share with Community"
                                        onClick={() => window.open(templatesSubmissionForm, '_blank')}
                                    />
                                )}

                                <DropdownMenuItem
                                    aria-label="Export Project"
                                    icon={<DownloadIcon />}
                                    label="Export"
                                    onClick={() =>
                                        (window.location.href = `/api/automation/internal/projects/${project.id}/export`)
                                    }
                                />

                                {ff_1039 && (canPullProjectFromGit || canConfigureProjectGit) && (
                                    <EEVersion hidden={true}>
                                        <DropdownMenuSeparator />

                                        {canPullProjectFromGit && (
                                            <DropdownMenuItem
                                                aria-label="Pull Project from Git"
                                                disabled={!projectGitConfiguration?.enabled}
                                                icon={<GitPullRequestArrowIcon />}
                                                label="Pull Project from Git"
                                                onClick={handlePullProjectFromGitClick}
                                            />
                                        )}

                                        {canConfigureProjectGit && (
                                            <DropdownMenuItem
                                                aria-label="Git Configuration"
                                                icon={<GitBranchIcon />}
                                                label="Git Configuration"
                                                onClick={() => setShowProjectGitConfigurationDialog(true)}
                                            />
                                        )}
                                    </EEVersion>
                                )}

                                {canDeleteProject && (
                                    <>
                                        <DropdownMenuSeparator />

                                        <DropdownMenuItem
                                            aria-label="Delete Project"
                                            icon={<Trash2Icon />}
                                            label="Delete"
                                            onClick={(event: MouseEvent) => {
                                                setShowDeleteDialog(true);

                                                event.stopPropagation();
                                            }}
                                            variant="destructive"
                                        />
                                    </>
                                )}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <AlertDialog
                ariaLabel="Confirm Project Deletion"
                description="This action cannot be undone. This will permanently delete the project and workflows it contains."
                isPending={deleteProjectMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => {
                    if (project.id) {
                        deleteProjectMutation.mutate(project.id);
                    }
                }}
                open={showDeleteDialog}
            />

            {showEditDialog && <ProjectDialog onClose={() => setShowEditDialog(false)} project={project} />}

            {projectDeploymentsSheetState && (
                <ProjectDeploymentsSheet
                    expandedProjectDeploymentId={projectDeploymentsSheetState.expandedProjectDeploymentId}
                    onSheetOpenChange={(open) => !open && setProjectDeploymentsSheetState(undefined)}
                    project={project}
                    sheetOpen
                />
            )}

            {showProjectGitConfigurationDialog && (
                <EEVersion hidden={true}>
                    <Suspense fallback={null}>
                        <ProjectGitConfigurationDialog
                            onClose={() => setShowProjectGitConfigurationDialog(false)}
                            onUpdateProjectGitConfigurationSubmit={handleUpdateProjectGitConfigurationSubmit}
                            projectGitConfiguration={projectGitConfiguration}
                            projectId={project.id!}
                        />
                    </Suspense>
                </EEVersion>
            )}

            {showProjectShareDialog && (
                <ProjectShareDialog
                    onOpenChange={() => setShowProjectShareDialog(false)}
                    open={showProjectShareDialog}
                    projectId={project.id!}
                    projectUuid={project.uuid!}
                    projectVersion={project.lastProjectVersion!}
                />
            )}

            {showPublishProjectDialog && !!project.id && (
                <ProjectPublishDialog onClose={() => setShowPublishProjectDialog(false)} project={project} />
            )}

            {showWorkflowDialog && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={project.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input
                accept=".json,.yaml,.yml"
                alt="file"
                className="hidden"
                data-testid={`${project.id}-importWorkflowHiddenInput`}
                onChange={(event) => handleImportWorkflow(event, project.id!, importProjectWorkflowMutation)}
                ref={hiddenFileInputRef}
                type="file"
            />

            <input
                accept=".json"
                className="hidden"
                onChange={async (event) => {
                    if (!event.target.files?.length) return;

                    try {
                        setIsImportingN8nWorkflow(true);
                        await handleImportN8nWorkflow(
                            event,
                            project.id!,
                            importProjectWorkflowMutation,
                            convertN8nWorkflow
                        );
                    } finally {
                        setIsImportingN8nWorkflow(false);

                        if (converterHiddenFileInputRef.current) {
                            converterHiddenFileInputRef.current.value = '';
                        }
                    }
                }}
                ref={converterHiddenFileInputRef}
                type="file"
            />
        </>
    );
};

export default ProjectListItem;
