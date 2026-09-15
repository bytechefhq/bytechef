import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import {ButtonGroup} from '@/components/ui/button-group';
import {PROJECTS_DOCUMENTATION_URL, getAutomationJourneySteps} from '@/pages/automation/journey/automationJourneySteps';
import {getAutomationJourney} from '@/pages/automation/journey/getAutomationJourney';
import loadProject from '@/pages/automation/project/loadProject';
import handleImportProject from '@/pages/automation/project/utils/handleImportProject';
import ProjectsFilterTitle from '@/pages/automation/projects/components/ProjectsFilterTitle';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {getProjectGitApi} from '@/shared/edition/project-git/projectGitApi';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import useButtonGroupDropdownAlign from '@/shared/hooks/useButtonGroupDropdownAlign';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import CategoryTagLeftSidebarNav from '@/shared/layout/CategoryTagLeftSidebarNav';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useImportProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {ProjectKeys, useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, FolderIcon, LayoutTemplateIcon, UploadIcon} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

import ProjectDialog from './components/ProjectDialog';
import ProjectList from './components/project-list/ProjectList';

export enum Type {
    Category,
    Tag,
}

// A workspace without projects is always at the first step.
const FIRST_RUN_JOURNEY = getAutomationJourney([], []);

const Projects = () => {
    const [newlyCreatedProjectId, setNewlyCreatedProjectId] = useState<number | undefined>();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const navigate = useNavigate();
    const {captureEmptyStateAction, captureProjectImported} = useAnalytics();

    const {alignOffset, buttonGroupRef, dropdownMenuTriggerRef, handleOpenChange} = useButtonGroupDropdownAlign();

    const queryClient = useQueryClient();

    const importProjectMutation = useImportProjectMutation({
        onSuccess: () => {
            captureProjectImported();

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast('Project is imported.');
        },
    });

    const ff_1039 = useFeatureFlagsStore()('ff-1039');

    const canCreateProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_CREATE');

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');

    const currentCategoryId = categoryId ? Number.parseInt(categoryId) : undefined;
    const currentTagId = tagId ? Number.parseInt(tagId) : undefined;

    const filterData = {
        id: currentCategoryId ?? currentTagId,
        type: tagId ? Type.Tag : Type.Category,
    };

    const isFiltered = filterData.id !== undefined;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {
        data: categories,
        error: categoriesError,
        isLoading: categoriesIsLoading,
    } = useGetProjectCategoriesQuery(currentWorkspaceId!);

    const {
        data: projectGitConfigurations,
        error: projectGitConfigurationsError,
        isLoading: projectGitConfigurationsIsLoading,
    } = getProjectGitApi().useWorkspaceProjectGitConfigurationsQuery(currentWorkspaceId!, ff_1039);

    const {
        data: projects,
        error: projectsError,
        isLoading: projectsIsLoading,
    } = useGetWorkspaceProjectsQuery({
        categoryId: currentCategoryId,
        id: currentWorkspaceId!,
        tagId: currentTagId,
    });

    const isRefetchingProjects =
        queryClient.isFetching({
            exact: true,
            queryKey: ProjectKeys.filteredProjects({
                categoryId: currentCategoryId,
                id: currentWorkspaceId!,
                tagId: currentTagId,
            }),
        }) > 0;

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetProjectTagsQuery(currentWorkspaceId!);

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const isFirstRun = !isFiltered && projects?.length === 0;

    useEffect(() => {
        loadProject();
    }, []);

    return (
        <LayoutContainer
            header={
                isFirstRun ? (
                    <Header centerTitle position="main" title="Projects" />
                ) : (
                    projects &&
                    projects.length > 0 && (
                        <Header
                            centerTitle={true}
                            position="main"
                            right={
                                canCreateProject && (
                                    <ButtonGroup>
                                        <ProjectDialog
                                            onSuccess={(projectId) => projectId && setNewlyCreatedProjectId(projectId)}
                                            project={undefined}
                                            triggerNode={
                                                <Button
                                                    aria-label="Create Project"
                                                    onSelect={(event) => event.preventDefault()}
                                                >
                                                    New Project
                                                </Button>
                                            }
                                        />

                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button>
                                                    <ChevronDownIcon />
                                                </Button>
                                            </DropdownMenuTrigger>

                                            <DropdownMenuContent align="end">
                                                <DropdownMenuItem
                                                    icon={<LayoutTemplateIcon />}
                                                    label="From Template"
                                                    onClick={() => navigate(`templates`)}
                                                />

                                                <DropdownMenuItem
                                                    icon={<UploadIcon />}
                                                    label="Import Project"
                                                    onClick={() => fileInputRef.current?.click()}
                                                />
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </ButtonGroup>
                                )
                            }
                            title={<ProjectsFilterTitle categories={categories} filterData={filterData} tags={tags} />}
                        />
                    )
                )
            }
            leftSidebarBody={
                <CategoryTagLeftSidebarNav
                    categories={categories}
                    categoriesIsLoading={categoriesIsLoading}
                    currentCategoryId={currentCategoryId}
                    currentTagId={currentTagId}
                    tags={tags}
                    tagsClassName="mb-0"
                    tagsEmptyMessage="No defined tags."
                    tagsIsLoading={tagsIsLoading}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Projects" />}
            leftSidebarHidden={isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[categoriesError, projectGitConfigurationsError, projectsError, tagsError]}
                loading={categoriesIsLoading || projectGitConfigurationsIsLoading || projectsIsLoading || tagsIsLoading}
            >
                {projects && projects?.length > 0 && tags ? (
                    <ProjectList
                        componentDefinitions={componentDefinitions}
                        isRefetchingProjects={isRefetchingProjects}
                        newlyCreatedProjectId={newlyCreatedProjectId}
                        projectGitConfigurations={projectGitConfigurations ?? []}
                        projects={projects}
                        tags={tags}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                    />
                ) : isFiltered ? (
                    <EmptyFilterResult entityName="projects" entityTitle="Projects" />
                ) : (
                    <EmptyState
                        actions={
                            canCreateProject && (
                                <ButtonGroup className="mx-auto" ref={buttonGroupRef}>
                                    <ProjectDialog
                                        onSuccess={(projectId) => projectId && setNewlyCreatedProjectId(projectId)}
                                        project={undefined}
                                        triggerNode={
                                            <Button
                                                aria-label="Create Project"
                                                label="Create Project"
                                                onClick={() =>
                                                    captureEmptyStateAction(
                                                        'projects',
                                                        'create_project',
                                                        FIRST_RUN_JOURNEY.stage
                                                    )
                                                }
                                            />
                                        }
                                    />

                                    <DropdownMenu onOpenChange={handleOpenChange}>
                                        <DropdownMenuTrigger asChild>
                                            <Button ref={dropdownMenuTriggerRef}>
                                                <ChevronDownIcon />
                                            </Button>
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="start" alignOffset={alignOffset}>
                                            <DropdownMenuItem
                                                icon={<LayoutTemplateIcon />}
                                                label="From Template"
                                                onClick={() => navigate(`templates`)}
                                            />

                                            <DropdownMenuItem
                                                icon={<UploadIcon />}
                                                label="Import Project"
                                                onClick={() => fileInputRef.current?.click()}
                                            />
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </ButtonGroup>
                            )
                        }
                        description="A project groups related workflows. Build and test a workflow inside it, then publish and deploy it so it runs on its own."
                        docsLink={{href: PROJECTS_DOCUMENTATION_URL, label: 'How projects work'}}
                        icon={<FolderIcon />}
                        title="Create your first project"
                    >
                        <JourneySteps
                            aria-label="Getting started"
                            steps={getAutomationJourneySteps(FIRST_RUN_JOURNEY.stepStatuses)}
                        />
                    </EmptyState>
                )}
            </PageLoader>

            <input
                accept=".zip"
                onChange={(event) => handleImportProject(event, currentWorkspaceId!, importProjectMutation)}
                ref={fileInputRef}
                style={{display: 'none'}}
                type="file"
            />
        </LayoutContainer>
    );
};

export default Projects;
