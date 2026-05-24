import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectCategoriesQuery,
    useAutomationWorkflowProjectTagsQuery,
    useAutomationWorkflowProjectsQuery,
    useCreateAutomationWorkflowProjectMutation,
    useCreateAutomationWorkflowProjectWorkflowMutation,
    useDeleteAutomationWorkflowProjectMutation,
    useDeleteAutomationWorkflowProjectWorkflowMutation,
    usePublishAutomationWorkflowProjectMutation,
    useUpdateAutomationWorkflowProjectMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {FolderIcon} from 'lucide-react';
import {ChangeEvent, useRef, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import AutomationWorkflowProjectsFilterTitle from './components/AutomationWorkflowProjectsFilterTitle';
import AutomationWorkflowDialog, {
    AutomationWorkflowFormValuesI,
} from './components/automation-workflow-dialog/AutomationWorkflowDialog';
import AutomationWorkflowProjectDialog, {
    AutomationWorkflowProjectFormValuesI,
} from './components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog';
import AutomationWorkflowProjectList from './components/automation-workflow-project-list/AutomationWorkflowProjectList';
import AutomationWorkflowProjectsFilterSidebar, {
    AutomationWorkflowProjectFilterType,
} from './components/automation-workflow-projects-filter-sidebar/AutomationWorkflowProjectsFilterSidebar';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

const AutomationWorkflows = () => {
    const [editProject, setEditProject] = useState<AutomationWorkflowProjectType | undefined>();
    const [pendingWorkflowProjectId, setPendingWorkflowProjectId] = useState<string | null>(null);
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

    const {data: categoriesData} = useAutomationWorkflowProjectCategoriesQuery();

    const {data: tagsData} = useAutomationWorkflowProjectTagsQuery();

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

    const createProjectMutation = useCreateAutomationWorkflowProjectMutation();
    const createWorkflowMutation = useCreateAutomationWorkflowProjectWorkflowMutation();
    const deleteProjectMutation = useDeleteAutomationWorkflowProjectMutation();
    const deleteWorkflowMutation = useDeleteAutomationWorkflowProjectWorkflowMutation();
    const publishProjectMutation = usePublishAutomationWorkflowProjectMutation();
    const updateProjectMutation = useUpdateAutomationWorkflowProjectMutation();

    const invalidateProjects = () => {
        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectCategories']});
        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectTags']});
        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
    };

    const openWorkflowEditor = (workflowUuid: string) => {
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
                    tags: values.tags,
                },
                {
                    onSuccess: () => {
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
            {definition, projectId: pendingWorkflowProjectId},
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
                        right={<Button aria-label="New Project" label="New Project" onClick={handleNewProject} />}
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
                <AutomationWorkflowProjectsFilterSidebar categories={categories} filterData={filterData} tags={tags} />
            }
            leftSidebarHeader={<Header title="Automation Workflows" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[projectsError]} loading={projectsIsLoading}>
                {filteredProjects.length > 0 ? (
                    <AutomationWorkflowProjectList
                        onCreateWorkflow={handleCreateWorkflow}
                        onDeleteProject={handleDeleteProject}
                        onDeleteWorkflow={handleDeleteWorkflow}
                        onEditProject={handleEditProject}
                        onImportWorkflow={handleImportWorkflow}
                        onPublishProject={handlePublishProject}
                        onSelectWorkflow={openWorkflowEditor}
                        onUpdateTags={handleUpdateTags}
                        projects={filteredProjects}
                        tags={tags || []}
                    />
                ) : (
                    <EmptyList
                        button={<Button label="Create Project" onClick={handleNewProject} />}
                        icon={<FolderIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new project."
                        title="No Projects"
                    />
                )}
            </PageLoader>

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
                <AutomationWorkflowDialog onClose={handleWorkflowDialogClose} onSubmit={handleWorkflowDialogSubmit} />
            )}

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
