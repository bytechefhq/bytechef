import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ProjectInstanceWorkflowSheet from '@/pages/automation/project-instances/components/ProjectInstanceWorkflowSheet';
import ProjectInstancesFilterTitle from '@/pages/automation/project-instances/components/ProjectInstancesFilterTitle';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Environment, ProjectInstance} from '@/shared/middleware/automation/configuration';
import {useGetProjectInstanceTagsQuery} from '@/shared/queries/automation/projectInstanceTags.queries';
import {useGetWorkspaceProjectInstancesQuery} from '@/shared/queries/automation/projectInstances.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {Layers3Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import ProjectInstanceDialog from './components/project-instance-dialog/ProjectInstanceDialog';
import ProjectInstanceList from './components/project-instance-list/ProjectInstanceList';

export enum Type {
    Project,
    Tag,
}

const ProjectInstances = () => {
    const [searchParams] = useSearchParams();

    const [environment, setEnvironment] = useState<number | undefined>(
        searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined
    );

    const {currentWorkspaceId} = useWorkspaceStore();

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
        projectInstances: true,
    });

    const {
        data: projectInstances,
        error: projectInstancesError,
        isLoading: projectInstancesIsLoading,
    } = useGetWorkspaceProjectInstancesQuery({
        environment:
            environment === undefined ? undefined : environment === 1 ? Environment.Test : Environment.Production,
        id: currentWorkspaceId!,
        projectId: searchParams.get('projectId') ? parseInt(searchParams.get('projectId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const projectInstanceMap: Map<number, ProjectInstance[]> = new Map<number, ProjectInstance[]>();

    if (projectInstances) {
        for (const projectInstance of projectInstances) {
            let currentProjectInstances: ProjectInstance[];

            if (projectInstance.project) {
                if (projectInstanceMap.has(projectInstance.projectId!)) {
                    currentProjectInstances = projectInstanceMap.get(projectInstance.projectId!)!;
                } else {
                    currentProjectInstances = [];
                }

                currentProjectInstances.push(projectInstance);

                projectInstanceMap.set(projectInstance.projectId!, currentProjectInstances);
            }
        }
    }

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetProjectInstanceTagsQuery();

    return (
        <LayoutContainer
            header={
                projectInstances &&
                projectInstances?.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <ProjectInstanceDialog
                                projectInstance={
                                    {
                                        environment:
                                            environment === undefined
                                                ? undefined
                                                : environment === 1
                                                  ? Environment.Test
                                                  : Environment.Production,
                                    } as ProjectInstance
                                }
                                triggerNode={<Button>New Instance</Button>}
                            />
                        }
                        title={
                            <ProjectInstancesFilterTitle
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
                                            onItemClick: (id?: number | string) => {
                                                setEnvironment(id as number);
                                            },
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
            leftSidebarHeader={<Header position="sidebar" title="Instances" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[projectsError, projectInstancesError, tagsError]}
                loading={projectsIsLoading || projectInstancesIsLoading || tagsIsLoading}
            >
                {projectInstances && projectInstances?.length > 0 ? (
                    <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                        {Array.from(projectInstanceMap.keys())?.map(
                            (projectId) =>
                                projects &&
                                tags && (
                                    <ProjectInstanceList
                                        key={projectId}
                                        project={projects.find((currentProject) => currentProject.id === projectId)!}
                                        projectInstances={projectInstanceMap.get(projectId)!}
                                        tags={tags}
                                    />
                                )
                        )}

                        <ProjectInstanceWorkflowSheet />
                    </div>
                ) : (
                    <EmptyList
                        button={
                            <ProjectInstanceDialog
                                projectInstance={
                                    {
                                        environment: environment === 1 ? Environment.Test : Environment.Production,
                                    } as ProjectInstance
                                }
                                triggerNode={<Button>Create Instance</Button>}
                            />
                        }
                        icon={<Layers3Icon className="size-24 text-gray-300" />}
                        message="Get started by creating a new project instance."
                        title="No Project Instances"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ProjectInstances;
