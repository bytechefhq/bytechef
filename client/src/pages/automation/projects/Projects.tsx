import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {
    useGetProjectCategoriesQuery,
    useGetProjectTagsQuery,
} from '@/queries/projects.queries';
import {TagIcon} from '@heroicons/react/20/solid';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import LayoutContainer from '../../../layouts/LayoutContainer';
import PageHeader from '../../../layouts/PageHeader';
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

    const [filterData, setFilterData] = useState<{id?: number; type: Type}>(
        defaultCurrentState
    );

    const navigate = useNavigate();

    const {data: categories, isLoading: categoriesLoading} =
        useGetProjectCategoriesQuery();

    const {data: tags, isLoading: tagsLoading} = useGetProjectTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Category) {
        pageTitle = categories?.find(
            (category) => category.id === filterData.id
        )?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <LayoutContainer
            header={
                <PageHeader
                    centerTitle={true}
                    position="main"
                    right={
                        <ProjectDialog
                            project={undefined}
                            onClose={(project) => {
                                if (project) {
                                    navigate(
                                        `/automation/projects/${project?.id}/workflow/${project?.workflowIds![0]}`
                                    );
                                }
                            }}
                        />
                    }
                    title={`${
                        searchParams.get('tagId') ? 'Tags' : 'Categories'
                    }: ${pageTitle || 'All'}`}
                />
            }
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Projects" />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    topTitle="Categories"
                    topBody={
                        <>
                            <LeftSidebarNavItem
                                item={{
                                    filterData:
                                        !filterData?.id &&
                                        filterData.type === Type.Category,
                                    name: 'All Categories',
                                    onItemClick: (id?: number | string) => {
                                        setFilterData({
                                            id: id as number,
                                            type: Type.Category,
                                        });
                                    },
                                }}
                            />

                            {!categoriesLoading &&
                                categories?.map((item) => (
                                    <LeftSidebarNavItem
                                        key={item.name}
                                        item={{
                                            filterData:
                                                filterData?.id === item.id &&
                                                filterData.type ===
                                                    Type.Category,
                                            id: item.id,
                                            name: item.name,
                                            onItemClick: (
                                                id?: number | string
                                            ) => {
                                                setFilterData({
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
                                (tags?.length ? (
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
            <ProjectList />
        </LayoutContainer>
    );
};

export default Projects;
