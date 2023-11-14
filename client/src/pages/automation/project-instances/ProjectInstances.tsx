import EmptyList from '@/components/EmptyList/EmptyList';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {ProjectInstanceModel} from '@/middleware/helios/configuration';
import {useGetProjectInstanceTagsQuery} from '@/queries/projectInstanceTags.queries';
import {useGetProjectInstancesQuery} from '@/queries/projectInstances.queries';
import {useGetProjectsQuery} from '@/queries/projects.queries';
import {Layers3Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import LayoutContainer from '../../../layouts/LayoutContainer';
import PageHeader from '../../../layouts/PageHeader';
import ProjectInstanceDialog from './ProjectInstanceDialog';
import ProjectInstanceList from './ProjectInstanceList';

export enum Type {
    Project,
    Tag,
}

const ProjectInstances = () => {
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('projectId')
            ? parseInt(searchParams.get('projectId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Project,
    };

    const [filterData, setFilterData] = useState<{id?: number; type: Type}>(
        defaultCurrentState
    );

    const {data: projects, isLoading: projectsLoading} = useGetProjectsQuery({
        projectInstances: true,
    });

    const {data: projectInstances, isLoading: projectInstancesLoading} =
        useGetProjectInstancesQuery({
            projectId: searchParams.get('projectId')
                ? parseInt(searchParams.get('projectId')!)
                : undefined,
            tagId: searchParams.get('tagId')
                ? parseInt(searchParams.get('tagId')!)
                : undefined,
        });

    const projectInstanceMap: Map<number, ProjectInstanceModel[]> = new Map<
        number,
        ProjectInstanceModel[]
    >();

    if (projectInstances) {
        for (const projectInstance of projectInstances) {
            let currentProjectInstances: ProjectInstanceModel[];

            if (projectInstance.project) {
                if (projectInstanceMap.has(projectInstance.project.id!)) {
                    currentProjectInstances = projectInstanceMap.get(
                        projectInstance.project.id!
                    )!;
                } else {
                    currentProjectInstances = [];
                }

                currentProjectInstances.push(projectInstance);

                projectInstanceMap.set(
                    projectInstance.project.id!,
                    currentProjectInstances
                );
            }
        }
    }

    const {data: tags, isLoading: tagsLoading} =
        useGetProjectInstanceTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Project) {
        pageTitle = projects?.find((project) => project.id === filterData.id)
            ?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <LayoutContainer
            header={
                <PageHeader
                    centerTitle={true}
                    position="main"
                    right={<ProjectInstanceDialog />}
                    title={`${
                        searchParams.get('tagId') ? 'Tags' : 'Projects'
                    }: ${pageTitle || 'All'}`}
                />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    bottomBody={
                        <>
                            {!tagsLoading &&
                                (tags && !!tags.length ? (
                                    tags?.map((item) => (
                                        <LeftSidebarNavItem
                                            icon={
                                                <TagIcon className="mr-1 h-4 w-4" />
                                            }
                                            item={{
                                                filterData:
                                                    filterData?.id ===
                                                        item.id &&
                                                    filterData.type ===
                                                        Type.Tag,
                                                id: item.id!,
                                                name: item.name,
                                                onItemClick: (
                                                    id?: number | string
                                                ) => {
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
                                    <span className="px-3 text-xs">
                                        You have not created any tags yet.
                                    </span>
                                ))}
                        </>
                    }
                    bottomTitle="Tags"
                    topBody={
                        <>
                            <LeftSidebarNavItem
                                item={{
                                    filterData:
                                        !filterData?.id &&
                                        filterData.type === Type.Project,
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
                                                filterData?.id === item.id &&
                                                filterData.type ===
                                                    Type.Project,
                                            id: item.id,
                                            name: item.name,
                                            onItemClick: (
                                                id?: number | string
                                            ) => {
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
                    topTitle="Projects"
                />
            }
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Instances" />
            }
        >
            <div
                className={twMerge(
                    'w-full',
                    !projects?.length &&
                        'place-self-center px-2 2xl:mx-auto 2xl:w-4/5'
                )}
            >
                {!projectInstancesLoading &&
                !projectsLoading &&
                (!projects?.length || !projectInstances?.length) ? (
                    <EmptyList
                        button={<ProjectInstanceDialog />}
                        icon={
                            <Layers3Icon className="h-12 w-12 text-gray-400" />
                        }
                        message="Get started by creating a new project instance."
                        title="No instances of projects"
                    />
                ) : (
                    Array.from(projectInstanceMap.keys())?.map(
                        (projectId) =>
                            projects && (
                                <ProjectInstanceList
                                    key={projectId}
                                    project={
                                        projects.find(
                                            (currentProject) =>
                                                currentProject.id === projectId
                                        )!
                                    }
                                    projectInstances={
                                        projectInstanceMap.get(projectId)!
                                    }
                                />
                            )
                    )
                )}
            </div>
        </LayoutContainer>
    );
};

export default ProjectInstances;
