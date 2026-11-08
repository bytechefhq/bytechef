import PageLoader from '@/components/PageLoader';
import WebSearchProviderList from '@/pages/settings/platform/web-search-providers/components/WebSearchProviderList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useWebSearchProvidersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

const WebSearchProviders = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data, error, isLoading} = useWebSearchProvidersQuery({environment: String(currentEnvironmentId)});

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description="Enable the web search providers AI agents can use to search and read the web"
                        position="main"
                        title="Web Search"
                    />
                }
                leftSidebarOpen={false}
            >
                {data && (
                    <WebSearchProviderList
                        environment={currentEnvironmentId}
                        webSearchProviders={data.webSearchProviders}
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default WebSearchProviders;
