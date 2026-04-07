import PageLoader from '@/components/PageLoader';
import AiProviderList from '@/ee/pages/settings/platform/ai-providers/components/AiProviderList';
import {useGetAiProvidersQuery} from '@/ee/shared/queries/platform/aiProviders.queries';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
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
                        description="Enable providers used by Univerzal AI Connectors"
                        position="main"
                        right={<EnvironmentSelect />}
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
