import EmptyList from '@/components/EmptyList/EmptyList';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {ProjectInstanceModel} from '@/middleware/helios/configuration';
import {
    useGetProjectInstanceTagsQuery,
    useGetProjectInstancesQuery,
    useGetProjectsQuery,
} from '@/queries/projects.queries';
import {TagIcon} from '@heroicons/react/20/solid';
import {FolderPlusIcon} from '@heroicons/react/24/outline';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

import LayoutContainer from '../../../layouts/LayoutContainer';
import PageHeader from '../../../layouts/PageHeader';
import ProjectInstanceDialog from './ProjectInstanceDialog';
import ProjectInstanceList from './ProjectInstanceList';

export enum Type {
    Project,
    Tag,
}

export interface ProjectInstancesEnabledState {
    projectInstanceMap: Map<number, boolean>;
    setEnabled: (projectInstanceId: number, enabled: boolean) => void;
}

export const useProjectInstancesEnabledStore =
    create<ProjectInstancesEnabledState>()(
        devtools(
            (set) => ({
                projectInstanceMap: new Map<number, boolean>(),
                setEnabled: (projectInstanceId, enabled) =>
                    set((state) => ({
                        projectInstanceMap: new Map<number, boolean>(
                            state.projectInstanceMap.set(
                                projectInstanceId,
                                enabled
                            )
                        ),
                    })),
            }),
            {
                name: 'project-instances-enabled',
            }
        )
    );

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
            let curProjectInstances: ProjectInstanceModel[];

            if (projectInstance.project) {
                if (projectInstanceMap.has(projectInstance.project.id!)) {
                    curProjectInstances = projectInstanceMap.get(
                        projectInstance.project.id!
                    )!;
                } else {
                    curProjectInstances = [];
                }

                curProjectInstances.push(projectInstance);

                projectInstanceMap.set(
                    projectInstance.project.id!,
                    curProjectInstances
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
                    position="main"
                    right={<ProjectInstanceDialog />}
                    title={`${
                        searchParams.get('tagId') ? 'Tags' : 'Projects'
                    }: ${pageTitle || 'All'}`}
                />
            }
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Instances" />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    topTitle="Projects"
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
                                        key={item.name}
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
                                        toLink={`?projectId=${item.id}`}
                                    />
                                ))}
                        </>
                    }
                    bottomTitle="Tags"
                    bottomBody={
                        <>
                            {!tagsLoading &&
                                (tags && !!tags.length ? (
                                    tags?.map((item) => (
                                        <LeftSidebarNavItem
                                            key={item.id}
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
                                            icon={
                                                <TagIcon className="mr-1 h-4 w-4" />
                                            }
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
                />
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
                            <FolderPlusIcon className="h-12 w-12 text-gray-400" />
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
                                            (curProject) =>
                                                curProject.id === projectId
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
