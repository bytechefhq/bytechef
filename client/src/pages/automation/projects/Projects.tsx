import {TagIcon} from '@heroicons/react/20/solid';
import React, {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import PageHeader from '../../../components/PageHeader/PageHeader';
import LayoutContainer from '../../../layouts/LayoutContainer/LayoutContainer';
import LeftSidebarMenu from '../../../layouts/LeftSidebarMenu/LeftSidebarMenu';
import LeftSidebarMenuItem from '../../../layouts/LeftSidebarMenu/LeftSidebarMenuItem';
import {
    useGetProjectCategoriesQuery,
    useGetProjectTagsQuery,
} from '../../../queries/projects.queries';
import ProjectDialog from './ProjectDialog';
import ProjectList from './ProjectList';

export enum Type {
    Category,
    Tag,
}

const Projects = () => {
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : searchParams.get('tagId')
            ? parseInt(searchParams.get('tagId')!)
            : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const [current, setCurrent] = useState<{id?: number; type: Type}>(
        defaultCurrentState
    );

    const navigate = useNavigate();

    const {data: categories, isLoading: categoriesLoading} =
        useGetProjectCategoriesQuery();
    const {data: tags, isLoading: tagsLoading} = useGetProjectTagsQuery();

    const title =
        !categoriesLoading &&
        current.type === Type.Category &&
        current.id &&
        categories &&
        categories.length > 0
            ? categories.filter((category) => category.id === current.id)[0]
                  .name!
            : !tagsLoading &&
              current.type === Type.Tag &&
              tags &&
              tags.length > 0
            ? tags && tags.filter((tag) => tag.id === current.id)[0].name!
            : 'All Categories';

    return (
        <LayoutContainer
            bodyClassName="bg-white"
            header={
                <PageHeader
                    position="main"
                    right={
                        <ProjectDialog
                            project={undefined}
                            onClose={(project) => {
                                navigate(
                                    `/automation/projects/${
                                        project?.id
                                    }/workflow/${project?.workflowIds![0]}`
                                );
                            }}
                        />
                    }
                    title={title}
                />
            }
            leftSidebarHeader={<PageHeader leftSidebar title="Projects" />}
            leftSidebarBody={
                <LeftSidebarMenu
                    topTitle="Categories"
                    topBody={
                        <>
                            <LeftSidebarMenuItem
                                item={{
                                    current:
                                        !current?.id &&
                                        current.type === Type.Category,
                                    name: 'All Categories',
                                    onItemClick: (id?: number | string) => {
                                        setCurrent({
                                            id: id as number,
                                            type: Type.Category,
                                        });
                                    },
                                }}
                                toLink=""
                            />

                            {!categoriesLoading &&
                                categories?.map((item) => (
                                    <LeftSidebarMenuItem
                                        key={item.name}
                                        item={{
                                            current:
                                                current?.id === item.id &&
                                                current.type === Type.Category,
                                            id: item.id,
                                            name: item.name,
                                            onItemClick: (
                                                id?: number | string
                                            ) => {
                                                setCurrent({
                                                    id: id as number,
                                                    type: Type.Category,
                                                });
                                            },
                                        }}
                                        toLink={`?categoryId=${item.id}`}
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
            <ProjectList />
        </LayoutContainer>
    );
};

export default Projects;
