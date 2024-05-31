import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ProjectInstanceWorkflowSheet from '@/pages/automation/project-instances/components/ProjectInstanceWorkflowSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ProjectInstanceModel} from '@/shared/middleware/automation/configuration';
import {useGetProjectInstanceTagsQuery} from '@/shared/queries/automation/projectInstanceTags.queries';
import {useGetWorkspaceProjectInstancesQuery} from '@/shared/queries/automation/projectInstances.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {Layers3Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import ProjectInstanceDialog from './components/ProjectInstanceDialog';
import ProjectInstanceList from './components/ProjectInstanceList';

export enum Type {
    Project,
    Tag,
}

const ProjectInstances = () => {
    const {currentWorkspaceId} = useWorkspaceStore();

    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('projectId')
            ? parseInt(searchParams.get('projectId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Project,
    };

    const [filterData, setFilterData] = useState<{id?: number; type: Type}>(defaultCurrentState);

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

    let pageTitle: string | undefined;

    if (filterData.type === Type.Project) {
        pageTitle = projects?.find((project) => project.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

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
                                <LeftSidebarNavItem
                                    item={{
                                        filterData: !filterData?.id && filterData.type === Type.Project,
                                        name: 'All Projects',
                                        onItemClick: (id?: number | string) => {
                                            setFilterData({
                                                id: id as number,
                                                type: Type.Project,
                                            });
                                        },
                                    }}
                                />

                                {projects &&
                                    projects?.map((item) => (
                                        <LeftSidebarNavItem
                                            item={{
                                                filterData:
                                                    filterData?.id === item.id && filterData.type === Type.Project,
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
                                                    filterData:
                                                        filterData?.id === item.id && filterData.type === Type.Tag,
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
                                                toLink={`?tagId=${item.id}`}
                                            />
                                        ))
                                    ) : (
                                        <span className="px-3 text-xs">You have not created any tags yet.</span>
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

                        <ProjectInstanceWorkflowSheet />
                    </div>
                ) : (
                    <EmptyList
                        button={<ProjectInstanceDialog triggerNode={<Button>Create Instance</Button>} />}
                        icon={<Layers3Icon className="size-12 text-gray-400" />}
                        message="Get started by creating a new project instance."
                        title="No Project Instances"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ProjectInstances;
