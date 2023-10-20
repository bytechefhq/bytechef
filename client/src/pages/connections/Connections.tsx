import LayoutContainer from '../../layouts/LayoutContainer/LayoutContainer';
import PageHeader from '../../components/PageHeader/PageHeader';
import React, {useState} from 'react';
import LeftSidebarMenu from '../../layouts/LeftSidebarMenu/LeftSidebarMenu';
import {useSearchParams} from 'react-router-dom';
import LeftSidebarMenuItem from '../../layouts/LeftSidebarMenu/LeftSidebarMenuItem';
import {TagIcon} from '@heroicons/react/20/solid';
import {useGetComponentDefinitionsQuery} from '../../queries/componentDefinitions';
import {useGetConnectionTagsQuery} from '../../queries/connections';

export enum Type {
    Component,
    Tag,
}

const Connections = () => {
    const [searchParams] = useSearchParams();
    const [current, setCurrent] = useState<{id?: number | string; type: Type}>({
        id: searchParams.get('componentName')
            ? +searchParams.get('componentName')!
            : searchParams.get('tagId')
            ? +searchParams.get('tagId')!
            : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Component,
    });
    const {isLoading: componentsIsLoading, data: components} =
        useGetComponentDefinitionsQuery({authenticationInstances: true});
    const {isLoading: tagsIsLoading, data: tags} = useGetConnectionTagsQuery();

    return (
        <LayoutContainer
            header={<PageHeader position={'main'} title="All Connections" />}
            leftSidebarHeader={
                <PageHeader leftSidebar={true} title="Connections" />
            }
            leftSidebarBody={
                <LeftSidebarMenu
                    topTitle="Components"
                    topBody={
                        <>
                            <LeftSidebarMenuItem
                                item={{
                                    name: 'All Components',
                                    current:
                                        !current?.id &&
                                        current.type === Type.Component,
                                    onItemClick: (id?: number | string) => {
                                        setCurrent({id, type: Type.Component});
                                    },
                                }}
                                toLink={''}
                            />

                            {!componentsIsLoading &&
                                components?.map((item) => (
                                    <LeftSidebarMenuItem
                                        key={item.name}
                                        item={{
                                            id: item.name!,
                                            name: item.name!,
                                            current:
                                                current?.id === item.name &&
                                                current.type === Type.Component,
                                            onItemClick: (
                                                id?: number | string
                                            ) => {
                                                setCurrent({
                                                    id,
                                                    type: Type.Component,
                                                });
                                            },
                                        }}
                                        toLink={`?componentName=${item.name}`}
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
                                                        id,
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
        />
    );
};

export default Connections;
