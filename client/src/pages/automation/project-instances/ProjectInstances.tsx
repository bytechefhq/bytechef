import EmptyList from '@/components/EmptyList/EmptyList';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {
    useGetProjectInstanceTagsQuery,
    useGetProjectsQuery,
} from '@/queries/projects.queries';
import {TagIcon} from '@heroicons/react/20/solid';
import {FolderPlusIcon} from '@heroicons/react/24/outline';
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

    const [current, setCurrent] = useState<{id?: number; type: Type}>(
        defaultCurrentState
    );

    const {data: projects, isLoading: projectsLoading} = useGetProjectsQuery({
        projectInstances: true,
    });
    const {data: tags, isLoading: tagsLoading} =
        useGetProjectInstanceTagsQuery();

    const title =
        !projectsLoading &&
        current.type === Type.Project &&
        current.id &&
        projects &&
        projects.length > 0
            ? projects.filter((category) => category.id === current.id)[0].name!
            : !tagsLoading &&
              current.type === Type.Tag &&
              tags &&
              tags.length > 0
            ? tags && tags.filter((tag) => tag.id === current.id)[0].name!
            : 'All Projects';

    return (
        <LayoutContainer
            header={
                <PageHeader
                    position="main"
                    right={<ProjectInstanceDialog />}
                    title={title}
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
                                    current:
                                        !current?.id &&
                                        current.type === Type.Project,
                                    name: 'All Projects',
                                    onItemClick: (id?: number | string) => {
                                        setCurrent({
                                            id: id as number,
                                            type: Type.Project,
                                        });
                                    },
                                }}
                                toLink=""
                            />

                            {!projectsLoading &&
                                projects?.map((item) => (
                                    <LeftSidebarNavItem
                                        key={item.name}
                                        item={{
                                            current:
                                                current?.id === item.id &&
                                                current.type === Type.Project,
                                            id: item.id,
                                            name: item.name,
                                            onItemClick: (
                                                id?: number | string
                                            ) => {
                                                setCurrent({
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
                                                current:
                                                    current?.id === item.id &&
                                                    current.type === Type.Tag,
                                                id: item.id!,
                                                name: item.name,
                                                onItemClick: (
                                                    id?: number | string
                                                ) => {
                                                    setCurrent({
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
                    'w-full px-2 2xl:mx-auto 2xl:w-4/5',
                    projects?.length === 0 ? 'place-self-center' : ''
                )}
            >
                {!projectsLoading && projects?.length === 0 ? (
                    <EmptyList
                        button={<ProjectInstanceDialog />}
                        icon={
                            <FolderPlusIcon className="h-12 w-12 text-gray-400" />
                        }
                        message="Get started by creating a new project instance."
                        title="No instances of projects"
                    />
                ) : (
                    projects?.map((project) => (
                        <ProjectInstanceList
                            key={project.id}
                            project={project}
                        />
                    ))
                )}
            </div>
        </LayoutContainer>
    );
};

export default ProjectInstances;
