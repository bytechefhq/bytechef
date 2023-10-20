import {TagIcon} from '@heroicons/react/20/solid';
import React, {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import PageHeader from '../../../components/PageHeader/PageHeader';
import LayoutContainer from '../../../layouts/LayoutContainer/LayoutContainer';
import LeftSidebarMenu from '../../../layouts/LeftSidebarMenu/LeftSidebarMenu';
import LeftSidebarMenuItem from '../../../layouts/LeftSidebarMenu/LeftSidebarMenuItem';
import {useGetComponentDefinitionsQuery} from '../../../queries/componentDefinitions.queries';
import {useGetConnectionTagsQuery} from '../../../queries/connections.queries';
import ConnectionList from './ConnectionList';
import ConnectionDialog from './components/ConnectionDialog';

export enum Type {
    Component,
    Tag,
}

const Connections = () => {
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('componentName')
            ? parseInt(searchParams.get('componentName')!)
            : searchParams.get('tagId')
            ? parseInt(searchParams.get('tagId')!)
            : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Component,
    };

    const [current, setCurrent] = useState<{id?: number | string; type: Type}>(
        defaultCurrentState
    );
    const {data: components, isLoading: componentsLoading} =
        useGetComponentDefinitionsQuery({connectionInstances: true});
    const {data: tags, isLoading: tagsLoading} = useGetConnectionTagsQuery();

    const title: string = getTitle();

    function getTitle(): string {
        if (
            !componentsLoading &&
            current.type === Type.Component &&
            current.id &&
            components &&
            components.length > 0
        ) {
            return components.filter(
                (component) => component.name === current.id
            )[0].name!;
        } else if (
            !tagsLoading &&
            current.type === Type.Tag &&
            tags &&
            tags.length > 0
        ) {
            return tags && tags.filter((tag) => tag.id === current.id)[0].name!;
        } else {
            return 'All Components';
        }
    }

    return (
        <LayoutContainer
            bodyClassName="bg-white"
            header={
                <PageHeader
                    position="main"
                    right={<ConnectionDialog />}
                    title={title}
                />
            }
            leftSidebarHeader={<PageHeader leftSidebar title="Connections" />}
            leftSidebarBody={
                <LeftSidebarMenu
                    topTitle="Components"
                    topBody={
                        <>
                            <LeftSidebarMenuItem
                                item={{
                                    current:
                                        !current?.id &&
                                        current.type === Type.Component,
                                    name: 'All Components',
                                    onItemClick: (id?: number | string) => {
                                        setCurrent({id, type: Type.Component});
                                    },
                                }}
                                toLink=""
                            />

                            {!componentsLoading &&
                                components?.map((item) => (
                                    <LeftSidebarMenuItem
                                        key={item.name}
                                        item={{
                                            current:
                                                current?.id === item.name &&
                                                current.type === Type.Component,
                                            id: item.name!,
                                            name: item.name!,
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
                            {!tagsLoading &&
                                (!tags?.length ? (
                                    <p className="px-3 text-xs">No tags.</p>
                                ) : (
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
        >
            <ConnectionList />
        </LayoutContainer>
    );
};

export default Connections;
