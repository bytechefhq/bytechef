import PageLoader from '@/components/PageLoader';
import AiProviderList from '@/ee/pages/settings/platform/ai-providers/components/AiProviderList';
import {useGetAiProvidersQuery} from '@/ee/shared/queries/platform/aiProviders.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import * as React from 'react';

const AiProviders = () => {
    const {data: aiProviders, error: aiProvidersError, isLoading: aiProvidersLoading} = useGetAiProvidersQuery();

    return (
        <PageLoader errors={[aiProvidersError]} loading={aiProvidersLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description="Enable providers used by Univerzal AI Connectors"
                        position="main"
                        title="AI Providers"
                    />
                }
                leftSidebarOpen={false}
            >
                {aiProviders && <AiProviderList aiProviders={aiProviders} />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default AiProviders;
