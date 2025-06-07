import EmbeddedApp from '@/EmbeddedApp';
import WorkflowBuilder from '@/ee/pages/embedded/automations/WorkflowBuilder';
import {createBrowserRouter} from 'react-router-dom';

export const getRouter = () =>
    createBrowserRouter([
        {
            children: [
                {
                    element: <WorkflowBuilder />,
                    path: 'workflow-builder/:workflowReferenceCode',
                },
            ],
            element: <EmbeddedApp />,
            path: '/embedded',
        },
    ]);
