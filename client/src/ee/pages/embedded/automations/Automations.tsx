import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import ImportCodeWorkflowDialog from '@/pages/platform/code-workflow/components/ImportCodeWorkflowDialog';
import ProjectsFilterTitle from '@/pages/automation/projects/components/ProjectsFilterTitle';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    AutomationProjectsQuery,
    CodeWorkflowLanguage,
    useAutomationProjectCategoriesQuery,
    useAutomationProjectTagsQuery,
    useAutomationProjectsQuery,
    useCreateAutomationWorkflowProjectCodeWorkflowMutation,
    useCreateAutomationProjectMutation,
    useCreateAutomationProjectWorkflowMutation,
    useDeleteAutomationProjectMutation,
    useDeleteAutomationProjectWorkflowMutation,
    usePublishAutomationProjectMutation,
    useUpdateAutomationProjectMutation,
    useUpdateAutomationProjectWorkflowMutation,
    useUpdateAutomationProjectWorkflowPermissionExpressionMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, CodeIcon, FileUpIcon, FolderIcon, PlusIcon} from 'lucide-react';
import {ChangeEvent, useRef, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import AutomationDialog, {AutomationFormValuesI} from './components/automation-dialog/AutomationDialog';
import AutomationExecutionsSheet from './components/automation-executions-sheet/AutomationExecutionsSheet';
import AutomationProjectDialog, {
    AutomationProjectFormValuesI,
} from './components/automation-project-dialog/AutomationProjectDialog';
import AutomationProjectList from './components/automation-project-list/AutomationProjectList';
import AutomationProjectsFilterSidebar, {
    AutomationProjectFilterType,
} from './components/automation-projects-filter-sidebar/AutomationProjectsFilterSidebar';
import isEditableCodeLanguage from './utils/isEditableCodeLanguage';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

const Automations = () => {
    const [editProject, setEditProject] = useState<AutomationProjectType | undefined>();
    const [editWorkflow, setEditWorkflow] = useState<AutomationProjectType['workflowTemplates'][number] | undefined>();
    const [newlyCreatedProjectId, setNewlyCreatedProjectId] = useState<string | undefined>();
    const [pendingWorkflowProjectId, setPendingWorkflowProjectId] = useState<string | null>(null);
    const [showCodeProjectDialog, setShowCodeProjectDialog] = useState(false);
    const [showImportCodeWorkflowDialog, setShowImportCodeWorkflowDialog] = useState(false);
    const [showProjectDialog, setShowProjectDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const fileInputRef = useRef<HTMLInputElement>(null);
    const importProjectIdRef = useRef<string | null>(null);

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const queryClient = useQueryClient();

    const {
        data: projectsData,
        error: projectsError,
        isLoading: projectsIsLoading,
    } = useAutomationProjectsQuery({environmentId: String(DEVELOPMENT_ENVIRONMENT)});

    const {data: categoriesData, isLoading: categoriesIsLoading} = useAutomationProjectCategoriesQuery();

    const {data: tagsData, isLoading: tagsIsLoading} = useAutomationProjectTagsQuery();

    const initialLoadComplete = useInitialLoadComplete(projectsIsLoading || categoriesIsLoading || tagsIsLoading);

    const categories = categoriesData?.automationProjectCategories;
    const tags = tagsData?.automationProjectTags;

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: categoryId || tagId || undefined,
        type: tagId ? AutomationProjectFilterType.Tag : AutomationProjectFilterType.Category,
    };

    const projects = projectsData?.automationProjects || [];

    const filteredProjects = projects.filter((project) => {
        if (categoryId) {
            return project.categoryId === categoryId;
        }

        if (tagId) {
            return project.tagIds.includes(tagId);
        }

        return true;
    });

    const hasProjects = projects.length > 0;

    const hasActiveFilter = !!categoryId || !!tagId;

    const isFirstRun = !projectsIsLoading && !hasActiveFilter && !hasProjects;

    const createCodeWorkflowMutation = useCreateAutomationWorkflowProjectCodeWorkflowMutation();
    const createProjectMutation = useCreateAutomationProjectMutation();
    const createWorkflowMutation = useCreateAutomationProjectWorkflowMutation();
    const deleteProjectMutation = useDeleteAutomationProjectMutation();
    const deleteWorkflowMutation = useDeleteAutomationProjectWorkflowMutation();
    const publishProjectMutation = usePublishAutomationProjectMutation();
    const updateProjectMutation = useUpdateAutomationProjectMutation();
    const updateWorkflowMutation = useUpdateAutomationProjectWorkflowMutation();
    const updateWorkflowPermissionExpressionMutation = useUpdateAutomationProjectWorkflowPermissionExpressionMutation();

    const invalidateProjects = () => {
        void queryClient.invalidateQueries({queryKey: ['automationProjectCategories']});
        void queryClient.invalidateQueries({queryKey: ['automationProjectTags']});
        void queryClient.invalidateQueries({queryKey: ['automationProjects']});
    };

    const openWorkflowEditor = (workflowUuid: string) => {
        const codeProject = projects.find(
            (project) =>
                project.codeWorkflowProject &&
                isEditableCodeLanguage(project.codeWorkflowLanguage) &&
                project.workflowTemplates.some((workflowTemplate) => workflowTemplate.workflowUuid === workflowUuid)
        );

        if (codeProject) {
            void navigate(`/embedded/automations/projects/${codeProject.id}/code`);

            return;
        }

        void navigate(`/embedded/automations/${workflowUuid}/editor`);
    };

    const closeProjectDialog = () => {
        setShowProjectDialog(false);
        setEditProject(undefined);
    };

    const handleProjectDialogSubmit = (values: AutomationProjectFormValuesI) => {
        if (editProject) {
            updateProjectMutation.mutate(
                {
                    category: values.category || undefined,
                    description: values.description || undefined,
                    id: editProject.id,
                    name: values.name,
                    permissionExpression: values.permissionExpression,
                    tags: values.tags,
                },
                {
                    onSuccess: () => {
                        invalidateProjects();

                        closeProjectDialog();
                    },
                }
            );
        } else {
            createProjectMutation.mutate(
                {
                    category: values.category || undefined,
                    description: values.description || undefined,
                    name: values.name,
                    permissionExpression: values.permissionExpression,
                    tags: values.tags,
                },
                {
                    onSuccess: (data) => {
                        setNewlyCreatedProjectId(data.createAutomationProject);

                        invalidateProjects();

                        closeProjectDialog();
                    },
                }
            );
        }
    };

    const handleCreateWorkflow = (projectId: string) => {
        setPendingWorkflowProjectId(projectId);
        setShowWorkflowDialog(true);
    };

    const handleWorkflowDialogClose = () => {
        setShowWorkflowDialog(false);
        setPendingWorkflowProjectId(null);
        setEditWorkflow(undefined);
    };

    const handleWorkflowDialogSubmit = (values: AutomationFormValuesI) => {
        if (!pendingWorkflowProjectId) {
            return;
        }

        const definition = JSON.stringify({
            description: values.description,
            inputs: [],
            label: values.label,
            tasks: [],
            triggers: [],
        });

        createWorkflowMutation.mutate(
            {definition, permissionExpression: values.permissionExpression, projectId: pendingWorkflowProjectId},
            {
                onSuccess: (data) => {
                    invalidateProjects();

                    openWorkflowEditor(data.createAutomationProjectWorkflow);
                },
            }
        );

        setShowWorkflowDialog(false);
        setPendingWorkflowProjectId(null);
    };

    const handleEditWorkflow = (workflow: AutomationProjectType['workflowTemplates'][number]) => {
        setEditWorkflow(workflow);
        setShowWorkflowDialog(true);
    };

    const handleEditWorkflowSubmit = (values: AutomationFormValuesI) => {
        if (!editWorkflow) {
            return;
        }

        const workflowUuid = editWorkflow.workflowUuid;

        updateWorkflowMutation.mutate(
            {description: values.description, label: values.label, workflowUuid},
            {
                onSuccess: () => {
                    updateWorkflowPermissionExpressionMutation.mutate(
                        {permissionExpression: values.permissionExpression, workflowUuid},
                        {
                            onSuccess: () => {
                                invalidateProjects();

                                handleWorkflowDialogClose();
                            },
                        }
                    );
                },
            }
        );
    };

    const handleImportWorkflow = (projectId: string) => {
        importProjectIdRef.current = projectId;

        fileInputRef.current?.click();
    };

    const handleImportFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        const projectId = importProjectIdRef.current;

        event.target.value = '';
        importProjectIdRef.current = null;

        if (!file || !projectId) {
            return;
        }

        const definition = await file.text();

        createWorkflowMutation.mutate(
            {definition, projectId},
            {
                onSuccess: (data) => {
                    invalidateProjects();

                    openWorkflowEditor(data.createAutomationProjectWorkflow);
                },
            }
        );
    };

    const handleDeleteProject = (projectId: string) => {
        deleteProjectMutation.mutate({id: projectId}, {onSuccess: invalidateProjects});
    };

    const handleDeleteWorkflow = (workflowUuid: string) => {
        deleteWorkflowMutation.mutate({workflowUuid}, {onSuccess: invalidateProjects});
    };

    const handlePublishProject = (projectId: string) => {
        publishProjectMutation.mutate({id: projectId}, {onSuccess: invalidateProjects});
    };

    const handleEditProject = (project: AutomationProjectType) => {
        setEditProject(project);
        setShowProjectDialog(true);
    };

    const handleUpdateTags = (project: AutomationProjectType, tagNames: string[]) => {
        const categoryName = categories?.find((category) => category.id === project.categoryId)?.name;

        updateProjectMutation.mutate(
            {
                category: categoryName || undefined,
                description: project.description || undefined,
                id: project.id,
                name: project.name,
                tags: tagNames,
            },
            {onSuccess: invalidateProjects}
        );
    };

    const handleNewProject = () => {
        setEditProject(undefined);
        setShowProjectDialog(true);
    };

    return (
        <LayoutContainer
            header={
                hasProjects ? (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <ButtonGroup>
                                <Button aria-label="New Project" label="New Project" onClick={handleNewProject} />

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button aria-label="More project options">
                                            <ChevronDownIcon />
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <DropdownMenuItem onClick={handleNewProject}>
                                            <PlusIcon className="mr-2 size-4" />
                                            New Project
                                        </DropdownMenuItem>

                                        <DropdownMenuItem onClick={() => setShowCodeProjectDialog(true)}>
                                            <CodeIcon className="mr-2 size-4" />
                                            New Code Project
                                        </DropdownMenuItem>

                                        <DropdownMenuItem onClick={() => setShowImportCodeWorkflowDialog(true)}>
                                            <FileUpIcon className="mr-2 size-4" />
                                            Import Code Project
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </ButtonGroup>
                        }
                        title={
                            <ProjectsFilterTitle
                                categories={categories}
                                filterByTag={filterData.type === AutomationProjectFilterType.Tag}
                                filterId={filterData.id}
                                tags={tags}
                            />
                        }
                    />
                ) : (
                    isFirstRun && <Header centerTitle={true} position="main" title="Automations" />
                )
            }
            leftSidebarBody={
                <AutomationProjectsFilterSidebar
                    categories={categories}
                    categoriesIsLoading={categoriesIsLoading}
                    filterData={filterData}
                    tags={tags}
                    tagsIsLoading={tagsIsLoading}
                />
            }
            leftSidebarHeader={<Header title="Automations" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[projectsError]} loading={projectsIsLoading}>
                {filteredProjects.length > 0 ? (
                    <AutomationProjectList
                        newlyCreatedProjectId={newlyCreatedProjectId}
                        onCreateWorkflow={handleCreateWorkflow}
                        onDeleteProject={handleDeleteProject}
                        onDeleteWorkflow={handleDeleteWorkflow}
                        onEditProject={handleEditProject}
                        onEditProjectSource={(projectId) =>
                            navigate(`/embedded/automations/projects/${projectId}/code`)
                        }
                        onEditWorkflow={handleEditWorkflow}
                        onImportWorkflow={handleImportWorkflow}
                        onPublishProject={handlePublishProject}
                        onSelectWorkflow={openWorkflowEditor}
                        onUpdateTags={handleUpdateTags}
                        projects={filteredProjects}
                        tags={tags || []}
                    />
                ) : hasActiveFilter ? (
                    <EmptyList
                        icon={<FolderIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="No projects match the selected filter."
                        title="No Matching Projects"
                    />
                ) : (
                    <EmptyState
                        actions={
                            <ButtonGroup className="mx-auto">
                                <Button aria-label="Create Project" label="Create Project" onClick={handleNewProject} />

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button aria-label="More project options">
                                            <ChevronDownIcon />
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <DropdownMenuItem onClick={handleNewProject}>
                                            <PlusIcon className="mr-2 size-4" />
                                            New Project
                                        </DropdownMenuItem>

                                        <DropdownMenuItem onClick={() => setShowCodeProjectDialog(true)}>
                                            <CodeIcon className="mr-2 size-4" />
                                            New Code Project
                                        </DropdownMenuItem>

                                        <DropdownMenuItem onClick={() => setShowImportCodeWorkflowDialog(true)}>
                                            <FileUpIcon className="mr-2 size-4" />
                                            Import Code Project
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </ButtonGroup>
                        }
                        description="A project groups the workflow templates that ship with your product. Your connected users start from these templates and adjust their own copies in the embedded workflow builder."
                        icon={<FolderIcon />}
                        title="Create your first project"
                    />
                )}
            </PageLoader>

            {showImportCodeWorkflowDialog && (
                <ImportCodeWorkflowDialog
                    deployUrl="/api/embedded/internal/automation/projects/deploy"
                    description="Upload a built code project. If a project with the same name exists, it is redeployed as a new published version; connected users get it once an environment configuration points at it. Unpublished edits made in the editor are replaced."
                    fileFieldName="projectFile"
                    onClose={() => setShowImportCodeWorkflowDialog(false)}
                    onImported={invalidateProjects}
                    title="Import Code Project"
                />
            )}

            {showCodeProjectDialog && (
                <AutomationProjectDialog
                    categories={categories}
                    codeWorkflow
                    onClose={() => setShowCodeProjectDialog(false)}
                    onSubmit={(values) =>
                        createCodeWorkflowMutation.mutate(
                            {
                                category: values.category || undefined,
                                description: values.description || undefined,
                                language: values.language || CodeWorkflowLanguage.Javascript,
                                name: values.name,
                                permissionExpression: values.permissionExpression,
                                tags: values.tags,
                            },
                            {
                                onSuccess: (data) => {
                                    invalidateProjects();

                                    setShowCodeProjectDialog(false);

                                    navigate(
                                        `/embedded/automations/projects/${data.createAutomationWorkflowProjectCodeWorkflow}/code`
                                    );
                                },
                            }
                        )
                    }
                    tags={tags}
                />
            )}

            {showProjectDialog && (
                <AutomationProjectDialog
                    categories={categories}
                    onClose={closeProjectDialog}
                    onSubmit={handleProjectDialogSubmit}
                    project={editProject}
                    tags={tags}
                />
            )}

            {showWorkflowDialog && (
                <AutomationDialog
                    onClose={handleWorkflowDialogClose}
                    onSubmit={editWorkflow ? handleEditWorkflowSubmit : handleWorkflowDialogSubmit}
                    workflow={
                        editWorkflow
                            ? {
                                  description: editWorkflow.description,
                                  label: editWorkflow.label,
                                  permissionExpression: editWorkflow.permissionExpression,
                              }
                            : undefined
                    }
                />
            )}

            <AutomationExecutionsSheet />

            <input
                accept=".json,.yaml,.yml"
                className="hidden"
                onChange={handleImportFileChange}
                ref={fileInputRef}
                type="file"
            />
        </LayoutContainer>
    );
};

export default Automations;
