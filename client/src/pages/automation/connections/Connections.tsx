import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/queries/connections.queries';
import {Link2Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import LayoutContainer from '../../../layouts/LayoutContainer';
import PageHeader from '../../../layouts/PageHeader';
import ConnectionDialog from './components/ConnectionDialog';
import ConnectionList from './components/ConnectionList';

export enum Type {
    Component,
    Tag,
}

const Connections = () => {
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('componentName')
            ? searchParams.get('componentName')!
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Component,
    };

    const [filterData, setFilterData] = useState<{
        id?: number | string;
        type: Type;
    }>(defaultCurrentState);

    const {
        data: allConnections,
        error: allConnectionsError,
        isLoading: allConnectionsIsLoading,
    } = useGetConnectionsQuery({});

    const allComponentNames = allConnections?.map(
        (connection) => connection.componentName
    );

    const {data: componentDefinitions, isLoading: componentsLoading} =
        useGetComponentDefinitionsQuery(
            {include: allComponentNames},
            allComponentNames !== undefined
        );

    const {
        data: connections,
        error: connectionsError,
        isLoading: connectionsIsLoading,
    } = useGetConnectionsQuery({
        componentName: searchParams.get('componentName')
            ? searchParams.get('componentName')!
            : undefined,
        tagId: searchParams.get('tagId')
            ? parseInt(searchParams.get('tagId')!)
            : undefined,
    });

    const {
        data: tags,
        error: tagsError,
        isLoading: tagsIsLoading,
    } = useGetConnectionTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Component) {
        pageTitle = componentDefinitions?.find(
            (componentDefinition) => componentDefinition.name === filterData.id
        )?.title;
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
                        <ConnectionDialog
                            triggerNode={<Button>Create Connection</Button>}
                        />
                    }
                    title={`${
                        searchParams.get('tagId') ? 'Tags' : 'Components'
                    }: ${pageTitle || 'All'}`}
                />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    bottomBody={
                        <>
                            {!tagsIsLoading &&
                                (!tags?.length ? (
                                    <p className="px-3 text-xs">No tags.</p>
                                ) : (
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
                                                        id,
                                                        type: Type.Tag,
                                                    });
                                                },
                                            }}
                                            key={item.id}
                                            toLink={`?tagId=${item.id}`}
                                        />
                                    ))
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
                                componentDefinitions?.map((item) => (
                                    <LeftSidebarNavItem
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
                                        key={item.name}
                                        toLink={`?componentName=${item.name}`}
                                    />
                                ))}
                        </>
                    }
                    topTitle="Components"
                />
            }
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Connections" />
            }
        >
            <PageLoader
                errors={[allConnectionsError, connectionsError, tagsError]}
                loading={
                    allConnectionsIsLoading ||
                    connectionsIsLoading ||
                    tagsIsLoading
                }
            >
                {connections && connections?.length > 0 ? (
                    connections &&
                    tags && (
                        <ConnectionList connections={connections} tags={tags} />
                    )
                ) : (
                    <EmptyList
                        button={
                            <ConnectionDialog
                                triggerNode={<Button>Create Connection</Button>}
                            />
                        }
                        icon={<Link2Icon className="h-12 w-12 text-gray-400" />}
                        message="You do not have any Connections created yet."
                        title="No Connections"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Connections;
