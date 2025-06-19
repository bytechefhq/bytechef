import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ApiPlatformLeftSidebarNav from '@/ee/pages/automation/api-platform/ApiPlatformLeftSidebarNav';
import ApiCollectionDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionDialog';
import ApiCollectionsFilterTitle from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionsFilterTitle';
import {useGetApiCollectionTagsQuery} from '@/ee/queries/apiCollectionTags.queries';
import {useGetApiCollectionsQuery} from '@/ee/queries/apiCollections.queries';
import {Environment} from '@/ee/shared/middleware/automation/api-platform';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ReadOnlyWorkflowSheet from '@/shared/components/read-only-workflow-editor/ReadOnlyWorkflowSheet';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {Link2Icon} from 'lucide-react';
import {useLocation, useSearchParams} from 'react-router-dom';

import ApiCollectionList from './components/ApiCollectionList';

export enum Type {
    ApiKeys,
    Project,
    Tag,
}

const ApiCollections = () => {
    const {currentWorkspaceId} = useWorkspaceStore();

    const location = useLocation();
    const [searchParams] = useSearchParams();

    const environment = searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined;
    const filterData = location.pathname.includes('api-keys')
        ? {
              type: Type.ApiKeys,
          }
        : searchParams.get('projectId') || searchParams.get('tagId')
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
        apiCollections: true,
        id: currentWorkspaceId!,
        includeAllFields: false,
        projectDeployments: true,
    });

    const {
        data: apiCollections,
        error: apiCollectionsError,
        isLoading: apiCollectionsIsLoading,
    } = useGetApiCollectionsQuery({
        environment:
            environment === undefined
                ? undefined
                : environment === 1
                  ? Environment.Development
                  : environment === 2
                    ? Environment.Staging
                    : Environment.Production,
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
                <ApiPlatformLeftSidebarNav
                    environment={environment}
                    filterData={filterData}
                    projects={projects}
                    tags={tags}
                />
            }
            leftSidebarHeader={<Header title="API Collections" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[apiCollectionsError, projectsError, tagsError]}
                loading={apiCollectionsIsLoading || projectsIsLoading || tagsIsLoading}
            >
                {apiCollections && apiCollections?.length > 0 ? (
                    <div className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
                        <WorkflowReadOnlyProvider
                            value={{
                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                            }}
                        >
                            <ApiCollectionList apiCollections={apiCollections} tags={tags} />

                            <ReadOnlyWorkflowSheet />
                        </WorkflowReadOnlyProvider>
                    </div>
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
