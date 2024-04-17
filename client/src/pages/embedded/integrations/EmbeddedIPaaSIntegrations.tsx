import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import PageHeader from '@/layouts/PageHeader';
import {Type} from '@/pages/embedded/integrations/Integrations';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationList from '@/pages/embedded/integrations/components/IntegrationList';
import {useGetIntegrationCategoriesQuery} from '@/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/queries/embedded/integrationTags.quries';
import {useGetIntegrationsQuery} from '@/queries/embedded/integrations.queries';
import {SquareIcon} from 'lucide-react';
import {useLocation, useSearchParams} from 'react-router-dom';

const EmbeddedIPaaSIntegrations = () => {
    const {pathname} = useLocation();
    const [searchParams] = useSearchParams();

    const filterData = {
        id: pathname.includes('unified')
            ? searchParams.get('categoryId')!
            : searchParams.get('categoryId')
              ? parseInt(searchParams.get('categoryId')!)
              : searchParams.get('tagId')
                ? parseInt(searchParams.get('tagId')!)
                : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

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

    let pageTitle: string | undefined;

    if (filterData.type === Type.Category) {
        pageTitle = categories?.find((category) => category.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <PageLoader
            errors={[categoriesError, integrationsError, tagsError]}
            loading={categoriesLoading || integrationsLoading || tagsLoading}
        >
            {integrations && integrations?.length > 0 && tags ? (
                <div className="flex size-full flex-col">
                    <PageHeader
                        centerTitle={true}
                        position="main"
                        right={
                            integrations &&
                            integrations.length > 0 && (
                                <IntegrationDialog
                                    integration={undefined}
                                    triggerNode={<Button>New Integration</Button>}
                                />
                            )
                        }
                        title={`${searchParams.get('tagId') ? 'Tag' : 'Category'}: ${pageTitle || 'All'}`}
                    />

                    <div className="flex-1 overflow-y-auto">
                        <IntegrationList integrations={integrations} tags={tags} />
                    </div>
                </div>
            ) : (
                <EmptyList
                    button={
                        <IntegrationDialog integration={undefined} triggerNode={<Button>Create Integration</Button>} />
                    }
                    icon={<SquareIcon className="size-12 text-gray-400" />}
                    message="Get started by creating a new integrations."
                    title="No Integrations"
                />
            )}
        </PageLoader>
    );
};

export default EmbeddedIPaaSIntegrations;
