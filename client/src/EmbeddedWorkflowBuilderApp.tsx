import {Toaster} from '@/components/ui/sonner';
import useFetchInterceptor from '@/ee/pages/embedded/automation-workflows/workflow-builder/config/useFetchInterceptor';
import {Outlet} from 'react-router-dom';

const EmbeddedWorkflowBuilderApp = () => {
    useFetchInterceptor();

    return (
        <>
            <Outlet />
            <Toaster />
        </>
    );
};

export default EmbeddedWorkflowBuilderApp;
