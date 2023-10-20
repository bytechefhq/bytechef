import LayoutContainer from '../../layouts/LayoutContainer/LayoutContainer';
import IntegrationList from './IntegrationList';
import IntegrationModal from './IntegrationModal';
import React, {useState} from 'react';
import PageHeader from '../../components/PageHeader/PageHeader';
import LeftSidebarMenu from '../../layouts/LeftSidebarMenu/LeftSidebarMenu';
import LeftSidebarMenuItem from '../../layouts/LeftSidebarMenu/LeftSidebarMenuItem';
import {TagIcon} from '@heroicons/react/20/solid';
import {useSearchParams} from 'react-router-dom';
import {
    useGetIntegrationCategoriesQuery,
    useGetIntegrationTagsQuery,
} from '../../queries/integrations';

export enum Type {
    Category,
    Tag,
}

const Integrations = () => {
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('categoryId')
            ? +searchParams.get('categoryId')!
            : searchParams.get('tagId')
            ? +searchParams.get('tagId')!
            : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const [current, setCurrent] = useState<{id?: number; type: Type}>(
        defaultCurrentState
    );
    const {isLoading: categoriesIsLoading, data: categories} =
        useGetIntegrationCategoriesQuery();
    const {isLoading: tagsIsLoading, data: tags} = useGetIntegrationTagsQuery();

    const title =
        !categoriesIsLoading &&
        current.type === Type.Category &&
        current.id &&
        categories &&
        categories.length > 0
            ? categories.filter((category) => category.id === current.id)[0]
                  .name!
            : !tagsIsLoading &&
              current.type === Type.Tag &&
              tags &&
              tags.length > 0
            ? tags && tags.filter((tag) => tag.id === current.id)[0].name!
            : 'All Integrations';

    return (
        <LayoutContainer
            header={
                <PageHeader
                    position="main"
                    right={<IntegrationModal />}
                    title={title}
                />
            }
            leftSidebarHeader={
                <PageHeader leftSidebar={true} title="Integrations" />
            }
            leftSidebarBody={
                <LeftSidebarMenu
                    topTitle="Components"
                    topBody={
                        <>
                            <LeftSidebarMenuItem
                                item={{
                                    name: 'All Categories',
                                    current:
                                        !current?.id &&
                                        current.type === Type.Category,
                                    onItemClick: (id?: number | string) => {
                                        setCurrent({
                                            id: id as number,
                                            type: Type.Category,
                                        });
                                    },
                                }}
                                toLink={''}
                            />

                            {!categoriesIsLoading &&
                                categories?.map((item) => (
                                    <LeftSidebarMenuItem
                                        key={item.name}
                                        item={{
                                            id: item.id,
                                            name: item.name,
                                            current:
                                                current?.id === item.id &&
                                                current.type === Type.Category,
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
                            {!tagsIsLoading &&
                                (!tags?.length ? (
                                    <p className="px-3 text-xs">No tags.</p>
                                ) : (
                                    tags?.map((item) => (
                                        <LeftSidebarMenuItem
                                            key={item.id}
                                            item={{
                                                id: item.id!,
                                                name: item.name,
                                                current:
                                                    current?.id === item.id &&
                                                    current.type === Type.Tag,
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
