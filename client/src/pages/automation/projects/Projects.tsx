import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import {ButtonGroup} from '@/components/ui/button-group';
import AgentsFilterLeftSidebarNav, {
    getAgentsFilter,
} from '@/pages/automation/agents/components/AgentsFilterLeftSidebarNav';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import isScheduledAgent from '@/pages/automation/agents/utils/isScheduledAgent';
import DataSyncDialog from '@/pages/automation/data-syncs/components/DataSyncDialog';
import DataSyncsFilterLeftSidebarNav, {
    getDataSyncsFilter,
} from '@/pages/automation/data-syncs/components/DataSyncsFilterLeftSidebarNav';
import useDataSyncs from '@/pages/automation/data-syncs/hooks/useDataSyncs';
import {PROJECTS_DOCUMENTATION_URL, getAutomationJourneySteps} from '@/pages/automation/journey/automationJourneySteps';
import {getAutomationJourney} from '@/pages/automation/journey/getAutomationJourney';
import loadProject from '@/pages/automation/project/loadProject';
import handleImportProject from '@/pages/automation/project/utils/handleImportProject';
import ProjectsFilterTitle from '@/pages/automation/projects/components/ProjectsFilterTitle';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {getProjectGitApi} from '@/shared/edition/project-git/projectGitApi';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import useButtonGroupDropdownAlign from '@/shared/hooks/useButtonGroupDropdownAlign';
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
import {ChevronDownIcon, FilterXIcon, FolderIcon, LayoutTemplateIcon, UploadIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
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

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');

    const currentCategoryId = categoryId ? Number.parseInt(categoryId) : undefined;
    const currentTagId = tagId ? Number.parseInt(tagId) : undefined;

    const filterData = {
        id: currentCategoryId ?? currentTagId,
        type: tagId ? Type.Tag : Type.Category,
    };

    const agentsFilter = getAgentsFilter(searchParams);
    const dataSyncsFilter = getDataSyncsFilter(searchParams);

    const isFiltered = filterData.id !== undefined || agentsFilter !== undefined || dataSyncsFilter !== undefined;

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

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetProjectTagsQuery();

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const isFirstRun = !isFiltered && projects?.length === 0;

    const {agents, agentsError, agentsIsLoading} = useAgents();

    const {dataSyncs, dataSyncsError, dataSyncsIsLoading} = useDataSyncs();

    const filteredProjects = useMemo(() => {
        if (!projects) {
            return projects;
        }

        if (agentsFilter) {
            const matchingAgents =
                agentsFilter === 'scheduled' ? agents.filter((agent) => isScheduledAgent(agent)) : agents;

            const agentProjectIds = new Set(matchingAgents.map((agent) => +agent.projectId));

            return projects.filter((project) => agentProjectIds.has(project.id!));
        }

        if (dataSyncsFilter) {
            const matchingDataSyncs =
                dataSyncsFilter === 'scheduled'
                    ? dataSyncs.filter((dataSync) => dataSync.triggerType === 'SCHEDULE')
                    : dataSyncs;

            const dataSyncProjectIds = new Set(matchingDataSyncs.map((dataSync) => +dataSync.projectId));

            return projects.filter((project) => dataSyncProjectIds.has(project.id!));
        }

        return projects;
    }, [agents, agentsFilter, dataSyncs, dataSyncsFilter, projects]);

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
                            }
                            title={
                                <ProjectsFilterTitle
                                    agentsFilter={agentsFilter}
                                    categories={categories}
                                    dataSyncsFilter={dataSyncsFilter}
                                    filterData={filterData}
                                    tags={tags}
                                />
                            }
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
                    middleGroups={
                        <>
                            <AgentsFilterLeftSidebarNav currentAgentsFilter={agentsFilter} />

                            <DataSyncsFilterLeftSidebarNav currentDataSyncsFilter={dataSyncsFilter} />
                        </>
                    }
                    otherFilterActive={agentsFilter !== undefined || dataSyncsFilter !== undefined}
                    preservedSearchParams={
                        agentsFilter
                            ? `agents=${agentsFilter}`
                            : dataSyncsFilter
                              ? `dataSyncs=${dataSyncsFilter}`
                              : undefined
                    }
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
                errors={[
                    categoriesError,
                    projectGitConfigurationsError,
                    projectsError,
                    tagsError,
                    agentsFilter ? agentsError : null,
                    dataSyncsFilter ? dataSyncsError : null,
                ]}
                loading={
                    categoriesIsLoading ||
                    projectGitConfigurationsIsLoading ||
                    projectsIsLoading ||
                    tagsIsLoading ||
                    (!!agentsFilter && agentsIsLoading) ||
                    (!!dataSyncsFilter && dataSyncsIsLoading)
                }
            >
                {filteredProjects && filteredProjects.length > 0 && tags ? (
                    <ProjectList
                        componentDefinitions={componentDefinitions}
                        defaultActiveTab={agentsFilter ? 'agents' : dataSyncsFilter ? 'dataSyncs' : 'workflows'}
                        isRefetchingProjects={isRefetchingProjects}
                        newlyCreatedProjectId={newlyCreatedProjectId}
                        projectGitConfigurations={projectGitConfigurations ?? []}
                        projects={filteredProjects}
                        tags={tags}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                    />
                ) : isFiltered ? (
                    dataSyncsFilter ? (
                        <EmptyList
                            button={<DataSyncDialog triggerNode={<Button label="Create Data Sync" />} />}
                            icon={<FilterXIcon className="size-24 text-stroke-neutral-tertiary" />}
                            message="No projects match the current filter."
                            title="No Matching Projects"
                        />
                    ) : (
                        <EmptyFilterResult entityName="projects" entityTitle="Projects" />
                    )
                ) : (
                    <EmptyState
                        actions={
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
