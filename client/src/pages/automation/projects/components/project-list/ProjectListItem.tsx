import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectGitConfiguration} from '@/ee/shared/middleware/automation/configuration';
import {
    usePullProjectFromGitMutation,
    useUpdateProjectGitConfigurationMutation,
} from '@/ee/shared/mutations/automation/projectGit.mutations';
import {ProjectGitConfigurationKeys} from '@/ee/shared/mutations/automation/projectGit.queries';
import {useToast} from '@/hooks/use-toast';
import ProjectGitConfigurationDialog from '@/pages/automation/project/components/ProjectGitConfigurationDialog';
import {ProjectShareDialog} from '@/pages/automation/project/components/ProjectShareDialog';
import ProjectPublishDialog from '@/pages/automation/projects/components/ProjectPublishDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import EEVersion from '@/shared/edition/EEVersion';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {useUpdateProjectTagsMutation} from '@/shared/mutations/automation/projectTags.mutations';
import {useDeleteProjectMutation, useDuplicateProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectCategoryKeys} from '@/shared/queries/automation/projectCategories.queries';
import {ProjectTagKeys} from '@/shared/queries/automation/projectTags.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
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
    PlusIcon,
    SendIcon,
    Share2Icon,
    Trash2Icon,
    UploadIcon,
    WorkflowIcon,
} from 'lucide-react';
import {ChangeEvent, useCallback, useRef, useState} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';

import TagList from '../../../../../shared/components/TagList';
import ProjectDialog from '../ProjectDialog';

interface ProjectItemProps {
    project: Project;
    projectGitConfiguration?: ProjectGitConfiguration;
    remainingTags?: Tag[];
}

const ProjectListItem = ({project, projectGitConfiguration, remainingTags}: ProjectItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showProjectGitConfigurationDialog, setShowProjectGitConfigurationDialog] = useState(false);
    const [showProjectShareDialog, setShowProjectShareDialog] = useState(false);
    const [showPublishProjectDialog, setShowPublishProjectDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const workflowsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);
    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.projects);

    const {captureProjectWorkflowCreated, captureProjectWorkflowImported} = useAnalytics();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const {toast} = useToast();

    const ff_1039 = useFeatureFlagsStore()('ff-1039');
    const ff_1041 = useFeatureFlagsStore()('ff-1041');
    const ff_1042 = useFeatureFlagsStore()('ff-1042');
    const ff_2482 = useFeatureFlagsStore()('ff-2482');
    const ff_2939 = useFeatureFlagsStore()('ff-2939');

    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (projectWorkflowId) => {
            captureProjectWorkflowCreated();

            navigate(`/automation/projects/${project.id}/project-workflows/${projectWorkflowId}?${searchParams}`);

            setShowWorkflowDialog(false);
        },
    });

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const importProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: () => {
            captureProjectWorkflowImported();

            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projects,
            });

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast({
                description: 'Workflow is imported.',
            });
        },
    });

    const pullProjectFromGitMutation = usePullProjectFromGitMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast({description: 'Project pulled from git repository successfully.'});
        },
    });

    const updateProjectGitConfigurationMutation = useUpdateProjectGitConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectGitConfigurationKeys.projectGitConfigurations,
            });
        },
    });

    const updateProjectTagsMutation = useUpdateProjectTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            importProjectWorkflowMutation.mutate({
                id: project.id!,
                workflow: {
                    definition: await event.target.files[0].text(),
                },
            });
        }
    };

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

    const handleProjectListItemClick = useCallback((event: React.MouseEvent) => {
        const target = event.target as HTMLElement;

        const interactiveSelectors = [
            '[data-interactive]',
            '.dropdown-menu-item',
            '[data-radix-dropdown-menu-item]',
            '[data-radix-dropdown-menu-trigger]',
            '[data-radix-collapsible-trigger]',
        ].join(', ');

        if (target.closest(interactiveSelectors)) {
            return;
        }

        if (workflowsCollapsibleTriggerRef.current?.contains(target)) {
            return;
        }

        workflowsCollapsibleTriggerRef.current?.click();
    }, []);

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-destructive-foreground"
                onClick={(event) => handleProjectListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center gap-2">
                            {project.projectWorkflowIds && project.projectWorkflowIds.length > 0 ? (
                                <Link
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
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger
                                    className="group mr-4 flex items-center text-xs font-semibold text-muted-foreground"
                                    ref={workflowsCollapsibleTriggerRef}
                                >
                                    <div className="mr-1">
                                        {project.projectWorkflowIds?.length === 1
                                            ? `${project.projectWorkflowIds?.length} workflow`
                                            : `${project.projectWorkflowIds?.length} workflows`}
                                    </div>

                                    <ChevronDownIcon className="duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

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
                                        <Badge className="flex space-x-1" variant="success">
                                            <span>V{project.lastProjectVersion - 1}</span>

                                            <span>PUBLISHED</span>
                                        </Badge>

                                        {/*<ProjectDeploymentDialog*/}
                                        {/*    projectDeployment={{*/}
                                        {/*        name: project.name,*/}
                                        {/*        projectId: project.id,*/}
                                        {/*    }}*/}
                                        {/*    triggerNode={*/}
                                        {/*        <Button*/}
                                        {/*            className="hover:bg-surface-neutral-primary-hover"*/}
                                        {/*            size="sm"*/}
                                        {/*            variant="ghost"*/}
                                        {/*        >*/}
                                        {/*            <RocketIcon /> Deploy*/}
                                        {/*        </Button>*/}
                                        {/*    }*/}
                                        {/*/>*/}
                                    </>
                                ) : (
                                    <Badge className="flex space-x-1" variant="secondary">
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
                                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" className="p-0">
                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowPublishProjectDialog(true)}
                                >
                                    <SendIcon /> Publish
                                </DropdownMenuItem>

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowEditDialog(true)}
                                >
                                    <EditIcon /> Edit
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => duplicateProjectMutation.mutate(project.id!)}
                                >
                                    <CopyIcon /> Duplicate
                                </DropdownMenuItem>

                                {project.projectWorkflowIds && project.projectWorkflowIds?.length > 0 && (
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() =>
                                            navigate(
                                                `/automation/projects/${project?.id}/project-workflows/${project?.projectWorkflowIds![0]}`
                                            )
                                        }
                                    >
                                        <WorkflowIcon /> View Workflows
                                    </DropdownMenuItem>
                                )}

                                {ff_1042 && (
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() => setShowProjectShareDialog(true)}
                                    >
                                        <Share2Icon /> Share Project
                                    </DropdownMenuItem>
                                )}

                                {ff_2939 && (
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() => {
                                            if (templatesSubmissionForm) {
                                                window.open(templatesSubmissionForm, '_blank');
                                            }
                                        }}
                                    >
                                        <Share2Icon /> Share with Community
                                    </DropdownMenuItem>
                                )}

                                {ff_2482 && (
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() =>
                                            (window.location.href = `/api/automation/internal/projects/${project.id}/export`)
                                        }
                                    >
                                        <DownloadIcon /> Export Project
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowWorkflowDialog(true)}
                                >
                                    <PlusIcon /> New Empty Workflow
                                </DropdownMenuItem>

                                {ff_1041 && (
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() => navigate(`${project.id}/templates`)}
                                    >
                                        <LayoutTemplateIcon /> Workflow from Template
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => {
                                        if (hiddenFileInputRef.current) {
                                            hiddenFileInputRef.current.click();
                                        }
                                    }}
                                >
                                    <UploadIcon /> Import Workflow
                                </DropdownMenuItem>

                                <DropdownMenuSeparator className="m-0" />

                                {ff_1039 && (
                                    <EEVersion hidden={true}>
                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            disabled={!projectGitConfiguration?.enabled}
                                            onClick={handlePullProjectFromGitClick}
                                        >
                                            <GitPullRequestArrowIcon /> Pull Project from Git
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={() => setShowProjectGitConfigurationDialog(true)}
                                        >
                                            <GitBranchIcon /> Git Configuration
                                        </DropdownMenuItem>

                                        <DropdownMenuSeparator className="m-0" />
                                    </EEVersion>
                                )}

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    onClick={() => setShowDeleteDialog(true)}
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the project and workflows it
                            contains..
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel className="shadow-none" onClick={() => setShowDeleteDialog(false)}>
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                            onClick={() => {
                                if (project.id) {
                                    deleteProjectMutation.mutate(project.id);
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && <ProjectDialog onClose={() => setShowEditDialog(false)} project={project} />}

            {showProjectGitConfigurationDialog && (
                <ProjectGitConfigurationDialog
                    onClose={() => setShowProjectGitConfigurationDialog(false)}
                    onUpdateProjectGitConfigurationSubmit={handleUpdateProjectGitConfigurationSubmit}
                    projectGitConfiguration={projectGitConfiguration}
                    projectId={project.id!}
                />
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

            {showWorkflowDialog && !!project.id && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    projectId={project.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input className="hidden" onChange={handleFileChange} ref={hiddenFileInputRef} type="file" />
        </>
    );
};

export default ProjectListItem;
