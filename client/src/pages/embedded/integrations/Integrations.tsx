import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationsFilterTitle from '@/pages/embedded/integrations/components/IntegrationsFilterTitle';
import IntegrationsLeftSidebarNav from '@/pages/embedded/integrations/components/IntegrationsLeftSidebarNav';
import IntegrationList from '@/pages/embedded/integrations/components/integration-list/IntegrationList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetIntegrationCategoriesQuery} from '@/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/shared/queries/embedded/integrationTags.quries';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
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
                                    triggerNode={<Button>New Integration</Button>}
                                />
                            )
                        }
                        title={<IntegrationsFilterTitle categories={categories} filterData={filterData} tags={tags} />}
                    />
                )
            }
            leftSidebarBody={<IntegrationsLeftSidebarNav categories={categories} filterData={filterData} tags={tags} />}
            leftSidebarHeader={<Header position="sidebar" title="Integrations" />}
            leftSidebarWidth="72"
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
                                triggerNode={<Button>Create Integration</Button>}
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
