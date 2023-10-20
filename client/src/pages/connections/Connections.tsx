import LayoutContainer from '../../layouts/LayoutContainer/LayoutContainer';
import PageHeader from '../../components/PageHeader/PageHeader';
import React, {useState} from 'react';
import LeftSidebarMenu from '../../layouts/LeftSidebarMenu/LeftSidebarMenu';
import {useSearchParams} from 'react-router-dom';
import LeftSidebarMenuItem from '../../layouts/LeftSidebarMenu/LeftSidebarMenuItem';
import {TagIcon} from '@heroicons/react/20/solid';
import {useGetComponentDefinitionsQuery} from '../../queries/componentDefinitions';
import {useGetConnectionTagsQuery} from '../../queries/connections';
import ConnectionDialog from './ConnectionDialog';
import ConnectionList from './ConnectionList';

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
    const {isLoading: componentsIsLoading, data: components} =
        useGetComponentDefinitionsQuery({connectionInstances: true});
    const {isLoading: tagsIsLoading, data: tags} = useGetConnectionTagsQuery();

    const title: string = getTitle();

    function getTitle(): string {
        if (
            !componentsIsLoading &&
            current.type === Type.Component &&
            current.id &&
            components &&
            components.length > 0
        ) {
            return components.filter(
                (component) => component.name === current.id
            )[0].name!;
        } else if (
            !tagsIsLoading &&
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
        >
            <ConnectionList />
        </LayoutContainer>
    );
};

export default Connections;
