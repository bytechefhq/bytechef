import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {useGetConnectionTagsQuery} from '@/queries/connections.queries';
import {TagIcon} from '@heroicons/react/20/solid';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import LayoutContainer from '../../../layouts/LayoutContainer';
import PageHeader from '../../../layouts/PageHeader';
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

    const [filterData, setFilterData] = useState<{
        id?: number | string;
        type: Type;
    }>(defaultCurrentState);

    const {data: components, isLoading: componentsLoading} =
        useGetComponentDefinitionsQuery({connectionInstances: true});

    const {data: tags, isLoading: tagsLoading} = useGetConnectionTagsQuery();

    const title: string = getTitle();

    function getTitle(): string {
        if (
            !componentsLoading &&
            filterData.type === Type.Component &&
            filterData.id &&
            components &&
            components.length > 0
        ) {
            return components.filter(
                (component) => component.name === filterData.id
            )[0].name!;
        } else if (
            !tagsLoading &&
            filterData.type === Type.Tag &&
            tags &&
            tags.length > 0
        ) {
            return (
                tags && tags.filter((tag) => tag.id === filterData.id)[0].name!
            );
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
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Connections" />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    topTitle="Components"
                    topBody={
                        <>
                            <LeftSidebarNavItem
                                item={{
                                    filterData:
                                        !filterData?.id &&
                                        filterData.type === Type.Component,
                                    name: 'All Components',
                                    onItemClick: (id?: number | string) => {
                                        setFilterData({
                                            id,
                                            type: Type.Component,
                                        });
                                    },
                                }}
                            />

                            {!componentsLoading &&
                                components?.map((item) => (
                                    <LeftSidebarNavItem
                                        key={item.name}
                                        item={{
                                            filterData:
                                                filterData?.id === item.name &&
                                                filterData.type ===
                                                    Type.Component,
                                            id: item.name!,
                                            name: item.title!,
                                            onItemClick: (
                                                id?: number | string
                                            ) =>
                                                setFilterData({
                                                    id,
                                                    type: Type.Component,
                                                }),
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
