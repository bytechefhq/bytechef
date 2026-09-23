import {Toaster} from '@/components/ui/sonner';
import {useIntegrationMarketplaceStore} from '@/ee/pages/embedded/integration-marketplace/stores/useIntegrationMarketplaceStore';
import {useEmbedHandshake} from '@/ee/pages/embedded/shared/useEmbedHandshake';
import useFetchInterceptor from '@/ee/pages/embedded/workflow-builder/config/useFetchInterceptor';
import {Outlet} from 'react-router-dom';

const EmbeddedIntegrationMarketplaceApp = () => {
    const initialize = useIntegrationMarketplaceStore((state) => state.initialize);

    useFetchInterceptor();
    useEmbedHandshake(initialize);

    return (
        <>
            <Outlet />

            <Toaster />
        </>
    );
};

export default EmbeddedIntegrationMarketplaceApp;
