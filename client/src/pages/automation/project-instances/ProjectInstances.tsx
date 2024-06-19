import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ProjectInstanceWorkflowSheet from '@/pages/automation/project-instances/components/ProjectInstanceWorkflowSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {EnvironmentModel, ProjectInstanceModel} from '@/shared/middleware/automation/configuration';
import {useGetProjectInstanceTagsQuery} from '@/shared/queries/automation/projectInstanceTags.queries';
import {useGetWorkspaceProjectInstancesQuery} from '@/shared/queries/automation/projectInstances.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {Layers3Icon, TagIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import ProjectInstanceDialog from './components/ProjectInstanceDialog';
import ProjectInstanceList from './components/ProjectInstanceList';
import useProjectInstanceWorkflowSheetStore from './stores/useProjectInstanceWorkflowSheetStore';

export enum Type {
    Project,
    Tag,
}

const ProjectInstances = () => {
    const [searchParams] = useSearchParams();

    const [environment, setEnvironment] = useState<number | undefined>(getEnvironment());
    const [filterData, setFilterData] = useState<{id?: number; type: Type}>(getFilterData());

    const {currentWorkspaceId} = useWorkspaceStore();

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
            environment === 1 ? EnvironmentModel.Test : environment === 2 ? EnvironmentModel.Production : undefined,
        id: currentWorkspaceId!,
        projectId: searchParams.get('projectId') ? parseInt(searchParams.get('projectId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const projectInstanceMap: Map<number, ProjectInstanceModel[]> = new Map<number, ProjectInstanceModel[]>();

    if (projectInstances) {
        for (const projectInstance of projectInstances) {
            let currentProjectInstances: ProjectInstanceModel[];

            if (projectInstance.project) {
                if (projectInstanceMap.has(projectInstance.project.id!)) {
                    currentProjectInstances = projectInstanceMap.get(projectInstance.project.id!)!;
                } else {
                    currentProjectInstances = [];
                }

                currentProjectInstances.push(projectInstance);

                projectInstanceMap.set(projectInstance.project.id!, currentProjectInstances);
            }
        }
    }

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetProjectInstanceTagsQuery();

    const {projectInstanceWorkflowSheetOpen} = useProjectInstanceWorkflowSheetStore();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Project) {
        pageTitle = projects?.find((project) => project.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    function getEnvironment() {
        return searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined;
    }

    function getFilterData() {
        return searchParams.get('projectId') || searchParams.get('tagId')
            ? {
                  id: searchParams.get('projectId')
                      ? parseInt(searchParams.get('projectId')!)
                      : parseInt(searchParams.get('tagId')!),
                  type: searchParams.get('tagId') ? Type.Tag : Type.Project,
              }
            : {type: Type.Project};
    }

    useEffect(() => {
        setEnvironment(getEnvironment());
        setFilterData(getFilterData());

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchParams]);

    return (
        <LayoutContainer
            header={
                projectInstances &&
                projectInstances?.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<ProjectInstanceDialog triggerNode={<Button>New Instance</Button>} />}
                        title={
                            !pageTitle
                                ? 'All Instances'
                                : `Filter by ${searchParams.get('tagId') ? 'tag' : 'project'}: ${pageTitle}`
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
                                    {label: 'All Environments', value: undefined},
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
                                        key={item.value}
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
                                        onItemClick: (id?: number | string) => {
                                            setFilterData({
                                                id: id as number,
                                                type: Type.Project,
                                            });
                                        },
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
                                                onItemClick: (id?: number | string) => {
                                                    setFilterData({
                                                        id: id as number,
                                                        type: Type.Project,
                                                    });
                                                },
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
                                                    onItemClick: (id?: number | string) => {
                                                        setFilterData({
                                                            id: id as number,
                                                            type: Type.Tag,
                                                        });
                                                    },
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

                        {projectInstanceWorkflowSheetOpen && <ProjectInstanceWorkflowSheet />}
                    </div>
                ) : (
                    <EmptyList
                        button={<ProjectInstanceDialog triggerNode={<Button>Create Instance</Button>} />}
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
