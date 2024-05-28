import App from '@/App';
import ErrorPage from '@/ErrorPage';
import {ProjectApi} from '@/middleware/automation/configuration';
import {Connections as AutomationConnections} from '@/pages/automation/connections/Connections';
import ProjectInstances from '@/pages/automation/project-instances/ProjectInstances';
import Project from '@/pages/automation/project/Project';
import Projects from '@/pages/automation/projects/Projects';
import {WorkflowExecutions as AutomationWorkflowExecutions} from '@/pages/automation/workflow-executions/WorkflowExecutions';
import OAuthPopup from '@/pages/platform/connection/components/oauth2/OAuthPopup';
import Account from '@/pages/platform/settings/Account';
import Appearance from '@/pages/platform/settings/Appearance';
import Settings from '@/pages/platform/settings/Settings';
import Workspaces from '@/pages/platform/settings/automation/workspaces/Workspaces';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter, redirect} from 'react-router-dom';

import {ProjectKeys} from './queries/automation/projects.queries';

const queryClient = new QueryClient();

export const router = createBrowserRouter([
    {
        element: <OAuthPopup />,
        path: '/callback',
    },
    {
        children: [
            {
                loader: async () => {
                    return redirect('automation/projects');
                },
                path: '',
            },
            {
                children: [
                    {
                        element: <Project />,
                        loader: async ({params}) =>
                            queryClient.ensureQueryData({
                                queryFn: () =>
                                    new ProjectApi().getProject({
                                        id: parseInt(params.projectId!),
                                    }),
                                queryKey: ProjectKeys.project(parseInt(params.projectId!)),
                            }),
                        path: 'projects/:projectId/project-workflows/:projectWorkflowId',
                    },
                    {
                        element: <Projects />,
                        path: 'projects',
                    },
                    {
                        element: <ProjectInstances />,
                        path: 'instances',
                    },
                    {
                        element: <AutomationConnections />,
                        path: 'connections',
                    },
                    {
                        element: <AutomationWorkflowExecutions />,
                        path: 'executions',
                    },
                    {
                        children: [
                            {
                                loader: async () => {
                                    return redirect('account');
                                },
                                path: '',
                            },
                            {
                                element: <Account />,
                                path: 'account',
                            },
                            {
                                element: <Appearance />,
                                path: 'appearance',
                            },
                            {
                                element: <Workspaces />,
                                path: 'workspaces',
                            },
                        ],
                        element: (
                            <Settings
                                sidebarNavItems={[
                                    {
                                        href: '/automation/settings/account',
                                        title: 'Account',
                                    },
                                    {
                                        href: '/automation/settings/appearance',
                                        title: 'Appearance',
                                    },
                                    {
                                        href: '/automation/settings/workspaces',
                                        title: 'Workspaces',
                                    },
                                ]}
                            />
                        ),
                        path: 'settings',
                    },
                ],
                path: 'automation',
            },
        ],
        element: <App />,
        errorElement: <ErrorPage />,
        path: '/',
    },
]);
