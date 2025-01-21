import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ProjectDeploymentFilterTitle from '@/pages/automation/project-deployments/components/ProjectDeploymentFilterTitle';
import ProjectDeploymentWorkflowSheet from '@/pages/automation/project-deployments/components/ProjectDeploymentWorkflowSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Environment, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useGetProjectDeploymentTagsQuery} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {Layers3Icon, TagIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

import ProjectDeploymentDialog from './components/project-deployment-dialog/ProjectDeploymentDialog';
import ProjectDeploymentList from './components/project-deployment-list/ProjectDeploymentList';

export enum Type {
    Project,
    Tag,
}

const ProjectDeployments = () => {
    const {currentWorkspaceId} = useWorkspaceStore();

    const [searchParams] = useSearchParams();

    const environment = searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined;
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
        id: currentWorkspaceId!,
        includeAllFields: false,
        projectDeployments: true,
    });

    const {
        data: projectDeployments,
        error: projectDeploymentsError,
        isLoading: projectDeploymentsIsLoading,
    } = useGetWorkspaceProjectDeploymentsQuery({
        environment:
            environment === undefined ? undefined : environment === 1 ? Environment.Test : Environment.Production,
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

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetProjectDeploymentTagsQuery();

    return (
        <LayoutContainer
            header={
                projectDeployments &&
                projectDeployments?.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <ProjectDeploymentDialog
                                projectDeployment={
                                    {
                                        environment:
                                            environment === undefined
                                                ? undefined
                                                : environment === 1
                                                  ? Environment.Test
                                                  : Environment.Production,
                                    } as ProjectDeployment
                                }
                                triggerNode={<Button>New Deployment</Button>}
                            />
                        }
                        title={
                            <ProjectDeploymentFilterTitle
                                environment={environment}
                                filterData={filterData}
                                projects={projects}
                                tags={tags}
                            />
                        }
                    />
                )
            }
            leftSidebarBody={
                <>
                    <LeftSidebarNav
                        body={
                            <>
                                {[
                                    {label: 'All Environments'},
                                    {label: 'Test', value: 1},
                                    {label: 'Production', value: 2},
                                ]?.map((item) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            current: environment === item.value,
                                            id: item.value,
                                            name: item.label,
                                        }}
                                        key={item.value ?? ''}
                                        toLink={`?environment=${item.value ?? ''}${filterData.id ? `&${filterData.type === Type.Project ? 'projectId' : 'tagId'}=${filterData.id}` : ''}`}
                                    />
                                ))}
                            </>
                        }
                        title="Environments"
                    />

                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        current: !filterData?.id && filterData.type === Type.Project,
                                        name: 'All Projects',
                                    }}
                                    toLink={`?environment=${environment ?? ''}`}
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
                                            toLink={`?projectId=${item.id}&environment=${environment ?? ''}`}
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
                                                toLink={`?tagId=${item.id}&environment=${environment ?? ''}`}
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
            leftSidebarHeader={<Header position="sidebar" title="Deployments" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[projectsError, projectDeploymentsError, tagsError]}
                loading={projectsIsLoading || projectDeploymentsIsLoading || tagsIsLoading}
            >
                {projectDeployments && projectDeployments?.length > 0 ? (
                    <div className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
                        {Array.from(projectDeploymentMap.keys())?.map(
                            (projectId) =>
                                projects &&
                                tags && (
                                    <ProjectDeploymentList
                                        key={projectId}
                                        project={projects.find((currentProject) => currentProject.id === projectId)!}
                                        projectDeployments={projectDeploymentMap.get(projectId)!}
                                        tags={tags}
                                    />
                                )
                        )}

                        <ProjectDeploymentWorkflowSheet />
                    </div>
                ) : (
                    <EmptyList
                        button={
                            <ProjectDeploymentDialog
                                projectDeployment={
                                    {
                                        environment: environment === 1 ? Environment.Test : Environment.Production,
                                    } as ProjectDeployment
                                }
                                triggerNode={<Button>Create Deployment</Button>}
                            />
                        }
                        icon={<Layers3Icon className="size-24 text-gray-300" />}
                        message="Get started by creating a new project deployment."
                        title="No Project Deployments"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ProjectDeployments;
