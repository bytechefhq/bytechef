import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useCreateConnectionMutation} from '@/shared/mutations/embedded/connections.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/shared/queries/embedded/connections.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Link2Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import ConnectionList from './components/ConnectionList';

export enum Type {
    Component,
    Tag,
}

export const Connections = () => {
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

    const allComponentNames = allConnections?.map((connection) => connection.componentName);

    const {data: components, isLoading: componentsLoading} = useGetComponentDefinitionsQuery(
        {include: allComponentNames ? allComponentNames : ['e']},
        allComponentNames !== undefined
    );

    const {
        data: connections,
        error: connectionsError,
        isLoading: connectionsIsLoading,
    } = useGetConnectionsQuery({
        componentName: searchParams.get('componentName') ? searchParams.get('componentName')! : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetConnectionTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Component) {
        pageTitle = components?.find((component) => component.name === filterData.id)?.title;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <LayoutContainer
            header={
                connections &&
                connections.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <ConnectionDialog
                                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                connectionsQueryKey={ConnectionKeys.connections}
                                triggerNode={<Button>New Connection</Button>}
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                            />
                        }
                        title={
                            !pageTitle
                                ? 'All Connections'
                                : `Filter by ${searchParams.get('tagId') ? 'tag' : 'component'}: ${pageTitle}`
                        }
                    />
                )
            }
            leftSidebarBody={
                <>
                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        current: !filterData?.id && filterData.type === Type.Component,
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
                                            item={{
                                                current:
                                                    filterData?.id === item.name && filterData.type === Type.Component,
                                                id: item.name!,
                                                name: item.title!,
                                                onItemClick: (id?: number | string) =>
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
                        title="Components"
                    />

                    <LeftSidebarNav
                        body={
                            <>
                                {!tagsIsLoading &&
                                    (!tags?.length ? (
                                        <p className="px-3 text-xs">No tags.</p>
                                    ) : (
                                        tags?.map((item) => (
                                            <LeftSidebarNavItem
                                                icon={<TagIcon className="mr-1 size-4" />}
                                                item={{
                                                    current: filterData?.id === item.id && filterData.type === Type.Tag,
                                                    id: item.id!,
                                                    name: item.name,
                                                    onItemClick: (id?: number | string) => {
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
                        title="Tags"
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Connections" />}
        >
            <PageLoader
                errors={[allConnectionsError, connectionsError, tagsError]}
                loading={allConnectionsIsLoading || connectionsIsLoading || tagsIsLoading}
            >
                {connections && connections?.length > 0 ? (
                    connections && tags && <ConnectionList connections={connections} tags={tags} />
                ) : (
                    <EmptyList
                        button={
                            <ConnectionDialog
                                connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                connectionsQueryKey={ConnectionKeys.connections}
                                triggerNode={<Button>Create Connection</Button>}
                                useCreateConnectionMutation={useCreateConnectionMutation}
                                useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                            />
                        }
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any Connections created yet."
                        title="No Connections"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
