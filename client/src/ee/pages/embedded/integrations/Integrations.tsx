import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationsFilterTitle from '@/ee/pages/embedded/integrations/components/IntegrationsFilterTitle';
import IntegrationsLeftSidebarNav from '@/ee/pages/embedded/integrations/components/IntegrationsLeftSidebarNav';
import IntegrationList from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationList';
import {useGetIntegrationCategoriesQuery} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {SquareIcon} from 'lucide-react';
import {useNavigate, useSearchParams} from 'react-router-dom';

export enum Type {
    Category,
    Tag,
    UnifiedAPI,
}

const Integrations = () => {
    const [searchParams] = useSearchParams();

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');

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

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    return (
        <LayoutContainer
            header={
                integrations &&
                integrations?.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            integrations &&
                            integrations.length > 0 && (
                                <IntegrationDialog
                                    integration={undefined}
                                    onClose={(integration) => {
                                        if (integration) {
                                            navigate(
                                                `/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}`
                                            );
                                        }
                                    }}
                                    triggerNode={<Button label="New Integration" />}
                                />
                            )
                        }
                        title={<IntegrationsFilterTitle categories={categories} filterData={filterData} tags={tags} />}
                    />
                )
            }
            leftSidebarBody={<IntegrationsLeftSidebarNav categories={categories} filterData={filterData} tags={tags} />}
            leftSidebarHeader={<Header position="sidebar" title="Integrations" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[categoriesError, integrationsError, tagsError]}
                loading={categoriesLoading || integrationsLoading || tagsLoading}
            >
                {integrations && integrations?.length > 0 && tags ? (
                    <IntegrationList integrations={integrations} tags={tags} />
                ) : (
                    <EmptyList
                        button={
                            <IntegrationDialog
                                integration={undefined}
                                onClose={(integration) => {
                                    if (integration) {
                                        navigate(
                                            `/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}`
                                        );
                                    }
                                }}
                                triggerNode={<Button label="Create Integration" />}
                            />
                        }
                        icon={<SquareIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new integrations."
                        title="No Integrations"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Integrations;
