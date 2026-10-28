import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import ImportCodeWorkflowDialog from '@/pages/platform/code-workflow/components/ImportCodeWorkflowDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    AutomationWorkflowProjectsQuery,
    CodeWorkflowLanguage,
    useAutomationWorkflowProjectCategoriesQuery,
    useAutomationWorkflowProjectTagsQuery,
    useAutomationWorkflowProjectsQuery,
    useCreateAutomationWorkflowProjectCodeWorkflowMutation,
    useCreateAutomationWorkflowProjectMutation,
    useCreateAutomationWorkflowProjectWorkflowMutation,
    useDeleteAutomationWorkflowProjectMutation,
    useDeleteAutomationWorkflowProjectWorkflowMutation,
    usePublishAutomationWorkflowProjectMutation,
    useUpdateAutomationWorkflowProjectMutation,
    useUpdateAutomationWorkflowProjectWorkflowMutation,
    useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, CodeIcon, FileUpIcon, FolderIcon, PlusIcon} from 'lucide-react';
import {ChangeEvent, useRef, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import AutomationWorkflowProjectsFilterTitle from './components/AutomationWorkflowProjectsFilterTitle';
import AutomationWorkflowDialog, {
    AutomationWorkflowFormValuesI,
} from './components/automation-workflow-dialog/AutomationWorkflowDialog';
import AutomationWorkflowExecutionsSheet from './components/automation-workflow-executions-sheet/AutomationWorkflowExecutionsSheet';
import AutomationWorkflowProjectDialog, {
    AutomationWorkflowProjectFormValuesI,
} from './components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog';
import AutomationWorkflowProjectList from './components/automation-workflow-project-list/AutomationWorkflowProjectList';
import AutomationWorkflowProjectsFilterSidebar, {
    AutomationWorkflowProjectFilterType,
} from './components/automation-workflow-projects-filter-sidebar/AutomationWorkflowProjectsFilterSidebar';
import isEditableCodeLanguage from './utils/isEditableCodeLanguage';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

const AutomationWorkflows = () => {
    const [editProject, setEditProject] = useState<AutomationWorkflowProjectType | undefined>();
    const [editWorkflow, setEditWorkflow] = useState<
        AutomationWorkflowProjectType['workflowTemplates'][number] | undefined
    >();
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
    } = useAutomationWorkflowProjectsQuery();

    const {data: categoriesData, isLoading: categoriesIsLoading} = useAutomationWorkflowProjectCategoriesQuery();

    const {data: tagsData, isLoading: tagsIsLoading} = useAutomationWorkflowProjectTagsQuery();

    const categories = categoriesData?.automationWorkflowProjectCategories;
    const tags = tagsData?.automationWorkflowProjectTags;

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: categoryId || tagId || undefined,
        type: tagId ? AutomationWorkflowProjectFilterType.Tag : AutomationWorkflowProjectFilterType.Category,
    };

    const projects = projectsData?.automationWorkflowProjects || [];

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

    const createCodeWorkflowMutation = useCreateAutomationWorkflowProjectCodeWorkflowMutation();
    const createProjectMutation = useCreateAutomationWorkflowProjectMutation();
    const createWorkflowMutation = useCreateAutomationWorkflowProjectWorkflowMutation();
    const deleteProjectMutation = useDeleteAutomationWorkflowProjectMutation();
    const deleteWorkflowMutation = useDeleteAutomationWorkflowProjectWorkflowMutation();
    const publishProjectMutation = usePublishAutomationWorkflowProjectMutation();
    const updateProjectMutation = useUpdateAutomationWorkflowProjectMutation();
    const updateWorkflowMutation = useUpdateAutomationWorkflowProjectWorkflowMutation();
    const updateWorkflowPermissionExpressionMutation =
        useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation();

    const invalidateProjects = () => {
        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectCategories']});
        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectTags']});
        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
    };

    const openWorkflowEditor = (workflowUuid: string) => {
        const codeProject = projects.find(
            (project) =>
                project.codeWorkflowProject &&
                isEditableCodeLanguage(project.codeWorkflowLanguage) &&
                project.workflowTemplates.some((workflowTemplate) => workflowTemplate.workflowUuid === workflowUuid)
        );

        if (codeProject) {
            navigate(`/embedded/automation-workflows/projects/${codeProject.id}/code`);

            return;
        }

        navigate(`/embedded/automation-workflows/${workflowUuid}/editor`);
    };

    const closeProjectDialog = () => {
        setShowProjectDialog(false);
        setEditProject(undefined);
    };

    const handleProjectDialogSubmit = (values: AutomationWorkflowProjectFormValuesI) => {
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
                        setNewlyCreatedProjectId(data.createAutomationWorkflowProject);

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

    const handleWorkflowDialogSubmit = (values: AutomationWorkflowFormValuesI) => {
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

                    openWorkflowEditor(data.createAutomationWorkflowProjectWorkflow);
                },
            }
        );

        setShowWorkflowDialog(false);
        setPendingWorkflowProjectId(null);
    };

    const handleEditWorkflow = (workflow: AutomationWorkflowProjectType['workflowTemplates'][number]) => {
        setEditWorkflow(workflow);
        setShowWorkflowDialog(true);
    };

    const handleEditWorkflowSubmit = (values: AutomationWorkflowFormValuesI) => {
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

                    openWorkflowEditor(data.createAutomationWorkflowProjectWorkflow);
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

    const handleEditProject = (project: AutomationWorkflowProjectType) => {
        setEditProject(project);
        setShowProjectDialog(true);
    };

    const handleUpdateTags = (project: AutomationWorkflowProjectType, tagNames: string[]) => {
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
                            <AutomationWorkflowProjectsFilterTitle
                                categories={categories}
                                filterData={filterData}
                                tags={tags}
                            />
                        }
                    />
                ) : undefined
            }
            leftSidebarBody={
                <AutomationWorkflowProjectsFilterSidebar
                    categories={categories}
                    categoriesIsLoading={categoriesIsLoading}
                    filterData={filterData}
                    tags={tags}
                    tagsIsLoading={tagsIsLoading}
                />
            }
            leftSidebarHeader={<Header title="Automation Workflows" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[projectsError]} loading={projectsIsLoading}>
                {filteredProjects.length > 0 ? (
                    <AutomationWorkflowProjectList
                        newlyCreatedProjectId={newlyCreatedProjectId}
                        onCreateWorkflow={handleCreateWorkflow}
                        onDeleteProject={handleDeleteProject}
                        onDeleteWorkflow={handleDeleteWorkflow}
                        onEditProject={handleEditProject}
                        onEditProjectSource={(projectId) =>
                            navigate(`/embedded/automation-workflows/projects/${projectId}/code`)
                        }
                        onEditWorkflow={handleEditWorkflow}
                        onImportWorkflow={handleImportWorkflow}
                        onPublishProject={handlePublishProject}
                        onSelectWorkflow={openWorkflowEditor}
                        onUpdateTags={handleUpdateTags}
                        projects={filteredProjects}
                        tags={tags || []}
                    />
                ) : (
                    <EmptyList
                        button={
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
                        icon={<FolderIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new project."
                        title="No Projects"
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
                <AutomationWorkflowProjectDialog
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
                                        `/embedded/automation-workflows/projects/${data.createAutomationWorkflowProjectCodeWorkflow}/code`
                                    );
                                },
                            }
                        )
                    }
                    tags={tags}
                />
            )}

            {showProjectDialog && (
                <AutomationWorkflowProjectDialog
                    categories={categories}
                    onClose={closeProjectDialog}
                    onSubmit={handleProjectDialogSubmit}
                    project={editProject}
                    tags={tags}
                />
            )}

            {showWorkflowDialog && (
                <AutomationWorkflowDialog
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

            <AutomationWorkflowExecutionsSheet />

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

export default AutomationWorkflows;
