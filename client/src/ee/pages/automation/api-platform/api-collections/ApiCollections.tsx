import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ApiCollectionDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionDialog';
import ApiCollectionsFilterTitle from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionsFilterTitle';
import {useGetApiCollectionTagsQuery} from '@/ee/queries/apiCollectionTags.queries';
import {useGetApiCollectionsQuery} from '@/ee/queries/apiCollections.queries';
import {Environment} from '@/middleware/automation/api-platform';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {Link2Icon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import ApiCollectionList from './components/ApiCollectionList';

export enum Type {
    Project,
    Tag,
}

const ApiCollections = () => {
    const [searchParams] = useSearchParams();

    const [environment, setEnvironment] = useState<number | undefined>(
        searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined
    );

    const {currentWorkspaceId} = useWorkspaceStore();

    const filterData =
        searchParams.get('projectId') || searchParams.get('tagId')
            ? {
                  id: searchParams.get('projectId')
                      ? parseInt(searchParams.get('projectId')!)
                      : parseInt(searchParams.get('tagId')!),
                  type: searchParams.get('tagId') ? Type.Tag : Type.Project,
              }
            : {type: Type.Project};

    const {
        data: projects,
        error: projectsError,
        isLoading: projectsIsLoading,
    } = useGetWorkspaceProjectsQuery({
        id: currentWorkspaceId!,
        projectInstances: true,
    });

    const {
        data: apiCollections,
        error: apiCollectionsError,
        isLoading: apiCollectionsIsLoading,
    } = useGetApiCollectionsQuery({
        environment:
            environment === undefined ? undefined : environment === 1 ? Environment.Test : Environment.Production,
        id: currentWorkspaceId!,
        projectId: searchParams.get('projectId') ? parseInt(searchParams.get('projectId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetApiCollectionTagsQuery();

    return (
        <LayoutContainer
            header={
                apiCollections &&
                apiCollections.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<ApiCollectionDialog triggerNode={<Button>New API Collection</Button>} />}
                        title={
                            <ApiCollectionsFilterTitle
                                environment={environment}
                                filterData={filterData}
                                projects={projects}
                                tags={tags}
                            />
                        }
                    />
                )
            }
            leftSidebarBody={
                <>
                    <LeftSidebarNav
                        body={
                            <>
                                {[
                                    {label: 'All Environments'},
                                    {label: 'Test', value: 1},
                                    {label: 'Production', value: 2},
                                ]?.map((item) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            current: environment === item.value,
                                            id: item.value,
                                            name: item.label,
                                            onItemClick: (id?: number | string) => {
                                                setEnvironment(id as number);
                                            },
                                        }}
                                        key={item.value ?? ''}
                                        toLink={`?environment=${item.value ?? ''}${filterData.id ? `&${filterData.type === Type.Project ? 'projectId' : 'tagId'}=${filterData.id}` : ''}`}
                                    />
                                ))}
                            </>
                        }
                        title="Environments"
                    />

                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        current: !filterData?.id && filterData.type === Type.Project,
                                        name: 'All Projects',
                                    }}
                                    toLink={`?environment=${environment ?? ''}`}
                                />

                                {projects &&
                                    projects?.map((item) => (
                                        <LeftSidebarNavItem
                                            item={{
                                                current: filterData?.id === item.id && filterData.type === Type.Project,
                                                id: item.id,
                                                name: item.name,
                                            }}
                                            key={item.name}
                                            toLink={`?projectId=${item.id}&environment=${environment ?? ''}`}
                                        />
                                    ))}
                            </>
                        }
                        title="Projects"
                    />

                    <LeftSidebarNav
                        body={
                            <>
                                {!tagsIsLoading &&
                                    (tags && !!tags.length ? (
                                        tags?.map((item) => (
                                            <LeftSidebarNavItem
                                                icon={<TagIcon className="mr-1 size-4" />}
                                                item={{
                                                    current: filterData?.id === item.id && filterData.type === Type.Tag,
                                                    id: item.id!,
                                                    name: item.name,
                                                }}
                                                key={item.id}
                                                toLink={`?tagId=${item.id}&environment=${environment ?? ''}`}
                                            />
                                        ))
                                    ) : (
                                        <span className="px-3 text-xs">No defined tags.</span>
                                    ))}
                            </>
                        }
                        title="Tags"
                    />
                </>
            }
            leftSidebarHeader={<Header title="API Collections" />}
            leftSidebarWidth="72"
        >
            <PageLoader
                errors={[apiCollectionsError, projectsError, tagsError]}
                loading={apiCollectionsIsLoading || projectsIsLoading || tagsIsLoading}
            >
                {apiCollections && apiCollections?.length > 0 ? (
                    <ApiCollectionList apiCollections={apiCollections} />
                ) : (
                    <EmptyList
                        button={<ApiCollectionDialog triggerNode={<Button>New API Collection</Button>} />}
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any API Collections created yet."
                        title="No API Collections"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ApiCollections;
