import EmbeddedWorkflowBuilderApp from '@/EmbeddedWorkflowBuilderApp';
import WorkflowBuilder from '@/ee/pages/embedded/automation-workflows/workflow-builder/WorkflowBuilder';
import {createBrowserRouter} from 'react-router-dom';

export const getRouter = () =>
    createBrowserRouter([
        {
            children: [
                {
                    element: <WorkflowBuilder />,
                    path: 'workflow-builder/:workflowUuid',
                },
            ],
            element: <EmbeddedWorkflowBuilderApp />,
            path: '/embedded',
        },
    ]);
