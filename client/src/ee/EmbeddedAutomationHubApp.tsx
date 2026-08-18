import {Toaster} from '@/components/ui/sonner';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {useEmbedHandshake} from '@/ee/pages/embedded/shared/useEmbedHandshake';
import useFetchInterceptor from '@/ee/pages/embedded/workflow-builder/config/useFetchInterceptor';
import {Outlet} from 'react-router-dom';

const EmbeddedAutomationHubApp = () => {
    const initialize = useAutomationHubStore((state) => state.initialize);

    useFetchInterceptor();
    useEmbedHandshake(initialize);

    return (
        <>
            <Outlet />
            <Toaster />
        </>
    );
};

export default EmbeddedAutomationHubApp;
