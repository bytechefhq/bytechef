import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationList from '@/pages/embedded/integrations/components/IntegrationList';
import {useGetIntegrationTagsQuery} from '@/queries/embedded/integrationTags.quries';
import {useGetIntegrationsQuery} from '@/queries/embedded/integrations.queries';
import {SquareIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

const EmbeddedIPaaSIntegrations = () => {
    const [searchParams] = useSearchParams();

    const {
        data: integrations,
        error: integrationsError,
        isLoading: integrationsLoading,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    return (
        <PageLoader errors={[integrationsError, tagsError]} loading={integrationsLoading || tagsLoading}>
            {integrations && integrations?.length > 0 && tags ? (
                <IntegrationList integrations={integrations} tags={tags} />
            ) : (
                <EmptyList
                    button={
                        <IntegrationDialog integration={undefined} triggerNode={<Button>Create Integration</Button>} />
                    }
                    icon={<SquareIcon className="size-12 text-gray-400" />}
                    message="Get started by creating a new integrations."
                    title="No integrations"
                />
            )}
        </PageLoader>
    );
};

export default EmbeddedIPaaSIntegrations;
