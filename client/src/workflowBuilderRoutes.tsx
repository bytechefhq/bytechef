import {Toaster} from '@/components/ui/toaster';
import WorkflowBuilder from '@/ee/pages/embedded/automations/WorkflowBuilder';
import {createBrowserRouter} from 'react-router-dom';

export const getRouter = () =>
    createBrowserRouter([
        {
            element: (
                <>
                    <WorkflowBuilder />
                    <Toaster />
                </>
            ),
            path: '/workflow-builder/:workflowReferenceCode',
        },
    ]);
