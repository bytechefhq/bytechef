import Button from '@/components/Button/Button';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import PageLoader from '@/components/PageLoader';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentFilterTitle from '@/pages/automation/project-deployments/components/ProjectDeploymentFilterTitle';
import ProjectDeploymentWorkflowExecutionsSheet from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectDeploymentTagsQuery} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {RefreshCwIcon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useLocation, useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import ProjectDeploymentsEmptyState from './components/ProjectDeploymentsEmptyState';
import ProjectDeploymentDialog from './components/project-deployment-dialog/ProjectDeploymentDialog';
import ProjectDeploymentList from './components/project-deployment-list/ProjectDeploymentList';

export enum Type {
    Project,
    Tag,
}

const ProjectDeploymentsSkeleton = () => (
    <div className="flex w-full items-center px-2 py-5">
        <div className="flex flex-1 flex-col gap-2">
            <Skeleton className="h-5 w-48" />

            <Skeleton className="h-4 w-32" />
        </div>

        <div className="flex items-center justify-end gap-x-6">
            <Skeleton className="h-5 w-8 shrink-0" />

            <div className="flex min-w-52 flex-col items-end gap-y-4">
                <Skeleton className="h-5 w-10 shrink-0 self-end" />

                <Skeleton className="h-5 w-20 shrink-0" />
            </div>

            <Skeleton className="size-9 shrink-0" />
        </div>
    </div>
);

const ProjectDeployments = () => {
    const [newlyCreatedDeploymentId, setNewlyCreatedDeploymentId] = useState<number | undefined>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const location = useLocation();
    const [searchParams] = useSearchParams();

    const canCreateDeployment = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_CREATE');

    const projectId = searchParams.get('projectId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: projectId ? parseInt(projectId) : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Project,
    };

    const isFiltered = filterData.id !== undefined;

    const {
        data: projects,
        error: projectsError,
        isLoading: projectsIsLoading,
    } = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
        projectDeployments: true,
    });

    const {
        data: projectDeployments,
        error: projectDeploymentsError,
        isFetching: projectDeploymentsIsFetching,
        isLoading: projectDeploymentsIsLoading,
        refetch: refetchProjectDeployments,
    } = useGetWorkspaceProjectDeploymentsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
        projectId: searchParams.get('projectId') ? parseInt(searchParams.get('projectId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const projectDeploymentMap: Map<number, ProjectDeployment[]> = new Map<number, ProjectDeployment[]>();

    if (projectDeployments) {
        for (const projectDeployment of projectDeployments) {
            let currentProjectDeployments: ProjectDeployment[];

            if (projectDeployment.project) {
                if (projectDeploymentMap.has(projectDeployment.projectId!)) {
                    currentProjectDeployments = projectDeploymentMap.get(projectDeployment.projectId!)!;
                } else {
                    currentProjectDeployments = [];
                }

                currentProjectDeployments.push(projectDeployment);

                projectDeploymentMap.set(projectDeployment.projectId!, currentProjectDeployments);
            }
        }
    }

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {
        data: tags,
        error: tagsError,
        isLoading: tagsIsLoading,
    } = useGetProjectDeploymentTagsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId!,
    });

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const expandedProjectDeploymentId =
        newlyCreatedDeploymentId ?? (location.state as {projectDeploymentId?: number} | null)?.projectDeploymentId;

    const isFirstRun = !isFiltered && projectDeployments?.length === 0;

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        projectDeployments &&
                        projectDeployments.length > 0 && (
                            <div className="flex items-center gap-x-2">
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            aria-label="Refresh deployments"
                                            disabled={projectDeploymentsIsFetching}
                                            icon={
                                                <RefreshCwIcon
                                                    className={twMerge(projectDeploymentsIsFetching && 'animate-spin')}
                                                />
                                            }
                                            onClick={() => refetchProjectDeployments()}
                                            size="icon"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Refresh</TooltipContent>
                                </Tooltip>

                                {canCreateDeployment && (
                                    <ProjectDeploymentDialog
                                        onSuccess={(deploymentId) => setNewlyCreatedDeploymentId(deploymentId)}
                                        projectDeployment={
                                            {
                                                environmentId: currentEnvironmentId,
                                            } as ProjectDeployment
                                        }
                                        redirectOnSubmit={false}
                                        triggerNode={<Button label="New Deployment" />}
                                    />
                                )}
                            </div>
                        )
                    }
                    title={
                        (projectDeployments && projectDeployments.length > 0) || isFiltered ? (
                            <ProjectDeploymentFilterTitle filterData={filterData} projects={projects} tags={tags} />
                        ) : (
                            isFirstRun && 'Deployments'
                        )
                    }
                />
            }
            leftSidebarBody={
                <>
                    <LeftSidebarFilterNav
                        items={(projects ?? []).map((project) => ({
                            current: filterData?.id === project.id && filterData.type === Type.Project,
                            id: project.id!,
                            name: project.name,
                            toLink: `?projectId=${project.id}`,
                        }))}
                        leadItem={{
                            current: !filterData?.id && filterData.type === Type.Project,
                            name: 'All Projects',
                        }}
                        loading={projectsIsLoading}
                        title="Projects"
                    />

                    <LeftSidebarFilterNav
                        emptyMessage="No defined tags."
                        icon={<TagIcon className="mr-1 size-4" />}
                        items={(tags ?? []).map((tag) => ({
                            current: filterData?.id === tag.id && filterData.type === Type.Tag,
                            id: tag.id!,
                            name: tag.name,
                            toLink: `?tagId=${tag.id}`,
                        }))}
                        loading={tagsIsLoading}
                        title="Tags"
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Deployments" />}
            leftSidebarHidden={isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[projectsError, projectDeploymentsError, tagsError]}
                loading={projectsIsLoading || projectDeploymentsIsLoading || tagsIsLoading}
            >
                {projectDeployments && projectDeployments?.length > 0 ? (
                    <div className="w-full divide-y divide-border/50 self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                        <WorkflowReadOnlyProvider
                            value={{
                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                            }}
                        >
                            {Array.from(projectDeploymentMap.keys())?.map(
                                (projectId) =>
                                    projects &&
                                    tags && (
                                        <ProjectDeploymentList
                                            componentDefinitions={componentDefinitions}
                                            key={projectId}
                                            newlyCreatedDeploymentId={expandedProjectDeploymentId}
                                            project={projects.find(
                                                (currentProject) => currentProject.id === projectId
                                            )!}
                                            projectDeployments={projectDeploymentMap.get(projectId)!}
                                            tags={tags}
                                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                                        />
                                    )
                            )}

                            {projectDeploymentsIsLoading &&
                                projectDeployments &&
                                (projectDeployments as ProjectDeployment[]).length > 0 && (
                                    <ProjectDeploymentsSkeleton />
                                )}

                            <ProjectDeploymentWorkflowExecutionsSheet />
                        </WorkflowReadOnlyProvider>
                    </div>
                ) : isFiltered ? (
                    <EmptyFilterResult entityName="project deployments" entityTitle="Project Deployments" />
                ) : (
                    <ProjectDeploymentsEmptyState
                        canCreateDeployment={canCreateDeployment}
                        onDeploymentCreated={setNewlyCreatedDeploymentId}
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ProjectDeployments;
