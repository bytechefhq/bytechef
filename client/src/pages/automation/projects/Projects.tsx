import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {useGetProjectCategoriesQuery} from '@/queries/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/queries/projectTags.quries';
import {TagIcon} from 'lucide-react';
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
                            onClose={(project) => {
                                if (project) {
                                    navigate(
                                        `/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`
                                    );
                                }
                            }}
                            project={undefined}
                        />
                    }
                    title={`${
                        searchParams.get('tagId') ? 'Tags' : 'Categories'
                    }: ${pageTitle || 'All'}`}
                />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    bottomBody={
                        <>
                            {!tagsLoading &&
                                (tags?.length ? (
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
                                        key={item.name}
                                        toLink={`?categoryId=${item.id}`}
                                    />
                                ))}
                        </>
                    }
                    topTitle="Categories"
                />
            }
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Projects" />
            }
        >
            <ProjectList />
        </LayoutContainer>
    );
};

export default Projects;
