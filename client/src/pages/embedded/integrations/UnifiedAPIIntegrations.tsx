import PageLoader from '@/components/PageLoader';
import UnifiedAPIIntegrationList from '@/pages/embedded/integrations/components/UnifiedAPIIntegrationList';
import {useGetComponentDefinitionsQuery} from '@/queries/platform/componentDefinitions.queries';

const UnifiedAPIIntegrations = () => {
    const {
        data: componentDefinitions,
        error: componentDefinitionsError,
        isLoading: componentDefinitionsLoading,
    } = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    return (
        <PageLoader errors={[componentDefinitionsError]} loading={componentDefinitionsLoading}>
            {componentDefinitions && <UnifiedAPIIntegrationList componentDefinitions={componentDefinitions} />}
        </PageLoader>
    );
};

export default UnifiedAPIIntegrations;
