import PageLoader from '@/components/PageLoader';
import AiProviderList from '@/pages/settings/platform/ai-providers/components/AiProviderList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetAiProvidersQuery} from '@/shared/queries/platform/aiProviders.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

const AiProviders = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        data: aiProviders,
        error: aiProvidersError,
        isLoading: aiProvidersLoading,
    } = useGetAiProvidersQuery(currentEnvironmentId);

    return (
        <PageLoader errors={[aiProvidersError]} loading={aiProvidersLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description="Enable providers used by Universal AI Connectors"
                        position="main"
                        title="AI Providers"
                    />
                }
                leftSidebarOpen={false}
            >
                {aiProviders && <AiProviderList aiProviders={aiProviders} environment={currentEnvironmentId} />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default AiProviders;
