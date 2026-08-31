import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Skeleton} from '@/components/ui/skeleton';
import ProjectDeploymentFilterTitle from '@/pages/automation/project-deployments/components/ProjectDeploymentFilterTitle';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import ReadOnlyWorkflowSheet from '@/shared/components/read-only-workflow-editor/ReadOnlyWorkflowSheet';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectDeploymentTagsQuery} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {
    ProjectDeploymentKeys,
    useGetWorkspaceProjectDeploymentsQuery,
} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {Layers3Icon, TagIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

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

    const [searchParams] = useSearchParams();

    const projectId = searchParams.get('projectId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: projectId ? parseInt(projectId) : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Project,
    };

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
        isLoading: projectDeploymentsIsLoading,
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
    } = useGetProjectDeploymentTagsQuery(currentWorkspaceId!);

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    // Refresh the deployment list after a BUILD-mode copilot turn creates or modifies a deployment, so the
    // page reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.PROJECT_DEPLOYMENT, () => {
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        ((projectDeployments && projectDeployments.length > 0) ||
                            !(projectsIsLoading || projectDeploymentsIsLoading || tagsIsLoading)) && (
                            <div className="flex items-center gap-1">
                                <CopilotButton source={Source.PROJECT_DEPLOYMENT} />

                                {projectDeployments && projectDeployments.length > 0 && (
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
                        projectDeployments && projectDeployments.length > 0 ? (
                            <ProjectDeploymentFilterTitle filterData={filterData} projects={projects} tags={tags} />
                        ) : (
                            ''
                        )
                    }
                />
            }
            leftSidebarBody={
                <>
                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        current: !filterData?.id && filterData.type === Type.Project,
                                        name: 'All Projects',
                                    }}
                                    toLink=""
                                />

                                {projects &&
                                    projects?.map((item) => (
                                        <LeftSidebarNavItem
                                            item={{
                                                current: filterData?.id === item.id && filterData.type === Type.Project,
                                                id: item.id,
                                                name: item.name,
                                            }}
                                            key={item.name}
                                            toLink={`?projectId=${item.id}`}
                                        />
                                    ))}
                            </>
                        }
                        title="Projects"
                    />

                    <LeftSidebarNav
                        body={
                            <>
                                {!tagsIsLoading &&
                                    (tags && !!tags.length ? (
                                        tags?.map((item) => (
                                            <LeftSidebarNavItem
                                                icon={<TagIcon className="mr-1 size-4" />}
                                                item={{
                                                    current: filterData?.id === item.id && filterData.type === Type.Tag,
                                                    id: item.id!,
                                                    name: item.name,
                                                }}
                                                key={item.id}
                                                toLink={`?tagId=${item.id}`}
                                            />
                                        ))
                                    ) : (
                                        <span className="px-3 text-xs">No defined tags.</span>
                                    ))}
                            </>
                        }
                        title="Tags"
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Project Deployments" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[projectsError, projectDeploymentsError, tagsError]}
                loading={projectsIsLoading || projectDeploymentsIsLoading || tagsIsLoading}
            >
                {projectDeployments && projectDeployments?.length > 0 ? (
                    <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
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
                                            newlyCreatedDeploymentId={newlyCreatedDeploymentId}
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

                            <ReadOnlyWorkflowSheet />
                        </WorkflowReadOnlyProvider>
                    </div>
                ) : (
                    <EmptyList
                        button={
                            <ProjectDeploymentDialog
                                onSuccess={(deploymentId) => setNewlyCreatedDeploymentId(deploymentId)}
                                projectDeployment={
                                    {
                                        environmentId: currentEnvironmentId,
                                    } as ProjectDeployment
                                }
                                redirectOnSubmit={false}
                                triggerNode={<Button>Create Deployment</Button>}
                            />
                        }
                        icon={<Layers3Icon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new project deployment."
                        title="No Project Deployments"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ProjectDeployments;
