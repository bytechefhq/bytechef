import {TagIcon} from '@heroicons/react/20/solid';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import PageHeader from '../../../components/PageHeader/PageHeader';
import LayoutContainer from '../../../layouts/LayoutContainer/LayoutContainer';
import LeftSidebarMenu from '../../../layouts/LeftSidebarMenu/LeftSidebarMenu';
import LeftSidebarMenuItem from '../../../layouts/LeftSidebarMenu/LeftSidebarMenuItem';
import {
    useGetProjectInstanceTagsQuery,
    useGetProjectsQuery,
} from '../../../queries/projects.queries';
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
            bodyClassName="bg-white"
            header={
                <PageHeader
                    position="main"
                    right={<ProjectInstanceDialog />}
                    title={title}
                />
            }
            leftSidebarHeader={<PageHeader leftSidebar title="Instances" />}
            leftSidebarBody={
                <LeftSidebarMenu
                    topTitle="Projects"
                    topBody={
                        <>
                            <LeftSidebarMenuItem
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
                                    <LeftSidebarMenuItem
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
                                        <LeftSidebarMenuItem
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
            <ProjectInstanceList />
        </LayoutContainer>
    );
};

export default ProjectInstances;
