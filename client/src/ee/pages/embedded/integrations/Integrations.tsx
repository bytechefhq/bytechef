import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import {EMBEDDED_INTEGRATIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationsFilterTitle from '@/ee/pages/embedded/integrations/components/IntegrationsFilterTitle';
import UnifiedApiLeftSidebarNav from '@/ee/pages/embedded/integrations/components/UnifiedApiLeftSidebarNav';
import IntegrationList from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationList';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationCategoriesQuery} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import CategoryTagLeftSidebarNav from '@/shared/layout/CategoryTagLeftSidebarNav';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SquareIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

export enum Type {
    Category,
    Tag,
    UnifiedAPI,
}

const INTEGRATION_STEPS = [
    {id: 'createIntegration', label: 'Create an integration', status: 'current' as const},
    {id: 'publishVersion', label: 'Publish a version', status: 'upcoming' as const},
    {id: 'createInstanceConfiguration', label: 'Create an instance configuration', status: 'upcoming' as const},
];

const Integrations = () => {
    const [newlyCreatedIntegrationId, setNewlyCreatedIntegrationId] = useState<number | undefined>();
    const [showIntegrationDialog, setShowIntegrationDialog] = useState(false);

    const [searchParams] = useSearchParams();

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');
    const unifiedApiCategory = searchParams.get('unifiedApiCategory') ?? undefined;

    const ff_743 = useFeatureFlagsStore()('ff-743');

    const filterData: {id: number | string | undefined; type: Type} = {
        id: categoryId ? parseInt(categoryId) : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Category,
    };

    const navigate = useNavigate();

    const {
        data: integrations,
        error: integrationsError,
        isLoading: integrationsLoading,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const initialLoadComplete = useInitialLoadComplete(integrationsLoading);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const hasActiveFilter = !!categoryId || !!tagId || !!unifiedApiCategory;

    const isFirstRun = !hasActiveFilter && integrations?.length === 0;

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        integrations &&
                        integrations.length > 0 && (
                            <Button
                                aria-label="Create Integration"
                                label="New Integration"
                                onClick={() => setShowIntegrationDialog(true)}
                            />
                        )
                    }
                    title={
                        (integrations && integrations.length > 0) || hasActiveFilter ? (
                            <IntegrationsFilterTitle categories={categories} filterData={filterData} tags={tags} />
                        ) : (
                            isFirstRun && 'Integrations'
                        )
                    }
                />
            }
            leftSidebarBody={
                <CategoryTagLeftSidebarNav
                    categories={categories}
                    categoriesIsLoading={categoriesLoading}
                    currentCategoryId={categoryId ? parseInt(categoryId) : undefined}
                    currentTagId={tagId ? parseInt(tagId) : undefined}
                    extraGroups={ff_743 && <UnifiedApiLeftSidebarNav currentUnifiedApiCategory={unifiedApiCategory} />}
                    otherFilterActive={!!unifiedApiCategory}
                    tags={tags}
                    tagsClassName={ff_743 ? '' : 'mb-0'}
                    tagsEmptyMessage="No tags."
                    tagsIsLoading={tagsLoading}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Integrations" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[categoriesError, integrationsError, tagsError]}
                loading={categoriesLoading || integrationsLoading || tagsLoading}
            >
                {integrations && integrations?.length > 0 && tags ? (
                    <IntegrationList
                        componentDefinitions={componentDefinitions}
                        integrations={integrations}
                        newlyCreatedIntegrationId={newlyCreatedIntegrationId}
                        tags={tags}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                    />
                ) : hasActiveFilter ? (
                    <EmptyList
                        icon={<SquareIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="No integrations match the selected filter."
                        title="No Matching Integrations"
                    />
                ) : (
                    <EmptyState
                        actions={
                            <Button
                                aria-label="Create Integration"
                                label="Create Integration"
                                onClick={() => setShowIntegrationDialog(true)}
                            />
                        }
                        description="An integration connects your product to a third-party app, like Slack or HubSpot. You build its workflows here, and your users turn it on from inside your product."
                        docsLink={{href: EMBEDDED_INTEGRATIONS_DOCUMENTATION_URL, label: 'How integrations work'}}
                        icon={<SquareIcon />}
                        title="Create your first integration"
                    >
                        <JourneySteps aria-label="Getting started" steps={INTEGRATION_STEPS} />
                    </EmptyState>
                )}
            </PageLoader>

            {showIntegrationDialog && (
                <IntegrationDialog
                    integration={undefined}
                    onClose={(integration) => {
                        setShowIntegrationDialog(false);

                        if (integration) {
                            navigate(
                                `/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}`
                            );
                        }
                    }}
                    onSuccess={(integrationId) => integrationId && setNewlyCreatedIntegrationId(integrationId)}
                />
            )}
        </LayoutContainer>
    );
};

export default Integrations;
