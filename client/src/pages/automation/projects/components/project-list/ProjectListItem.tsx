import '@/shared/styles/dropdownMenu.css';
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
import {Button} from '@/components/ui/button';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    usePullProjectFromGitMutation,
    useUpdateProjectGitConfigurationMutation,
} from '@/ee/mutations/projectGit.mutations';
import {ProjectGitConfigurationKeys} from '@/ee/queries/projectGit.queries';
import {ProjectGitConfiguration} from '@/ee/shared/middleware/automation/configuration';
import {useToast} from '@/hooks/use-toast';
import ProjectGitConfigurationDialog from '@/pages/automation/project/components/ProjectGitConfigurationDialog';
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
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ChevronDownIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {
    CopyIcon,
    DownloadIcon,
    EditIcon,
    EllipsisVerticalIcon,
    GitBranchIcon,
    GitPullRequestArrowIcon,
    PlusIcon,
    SendIcon,
    Trash2Icon,
    WorkflowIcon,
} from 'lucide-react';
import {ChangeEvent, useRef, useState} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';

import TagList from '../../../../../components/TagList';
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
    const [showPublishProjectDialog, setShowPublishProjectDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const {captureProjectWorkflowCreated, captureProjectWorkflowImported} = useAnalytics();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const {toast} = useToast();

    const ff_1039 = useFeatureFlagsStore()('ff-1039');

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

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-destructive-foreground">
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
                                <CollapsibleTrigger className="group mr-4 flex items-center text-xs font-semibold text-muted-foreground">
                                    <div className="mr-1">
                                        {project.projectWorkflowIds?.length === 1
                                            ? `${project.projectWorkflowIds?.length} workflow`
                                            : `${project.projectWorkflowIds?.length} workflows`}
                                    </div>

                                    <ChevronDownIcon className="duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.preventDefault()}>
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
                                {project.lastPublishedDate && project.lastVersion ? (
                                    <>
                                        <Badge className="flex space-x-1" variant="success">
                                            <span>V{project.lastVersion - 1}</span>

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
                                        <span>V{project.lastVersion}</span>

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
                                <Button size="icon" variant="ghost">
                                    <EllipsisVerticalIcon className="size-4 cursor-pointer" />
                                </Button>
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

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => {
                                        if (hiddenFileInputRef.current) {
                                            hiddenFileInputRef.current.click();
                                        }
                                    }}
                                >
                                    <DownloadIcon /> Import Workflow
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

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowWorkflowDialog(true)}
                                >
                                    <PlusIcon /> New Workflow
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
