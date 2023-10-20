import {
    useGetIntegrationCategoriesQuery,
    useGetIntegrationTagsQuery,
} from '@/ee/queries/integrations.queries';
import LayoutContainer from '@/layouts/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import PageHeader from '@/layouts/PageHeader';
import {TagIcon} from '@heroicons/react/20/solid';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import IntegrationDialog from './IntegrationDialog';
import IntegrationList from './IntegrationList';

export enum Type {
    Category,
    Tag,
}

const Integrations = () => {
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

    const {data: categories, isLoading: categoriesLoading} =
        useGetIntegrationCategoriesQuery();
    const {data: tags, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    const title =
        !categoriesLoading &&
        filterData.type === Type.Category &&
        filterData.id &&
        categories &&
        categories.length > 0
            ? categories.filter((category) => category.id === filterData.id)[0]
                  .name!
            : !tagsLoading &&
              filterData.type === Type.Tag &&
              tags &&
              tags.length > 0
            ? tags && tags.filter((tag) => tag.id === filterData.id)[0].name!
            : 'All Categories';

    return (
        <LayoutContainer
            header={
                <PageHeader
                    centerTitle={true}
                    position="main"
                    right={<IntegrationDialog integration={undefined} />}
                    title={title}
                />
            }
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Integrations" />
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
                                (!tags?.length ? (
                                    <p className="px-3 text-xs">No tags.</p>
                                ) : (
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
                                ))}
                        </>
                    }
                />
            }
        >
            <IntegrationList />
        </LayoutContainer>
    );
};

export default Integrations;
