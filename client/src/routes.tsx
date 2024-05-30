import App from '@/App';
import {Connections as AutomationConnections} from '@/pages/automation/connections/Connections';
import ProjectInstances from '@/pages/automation/project-instances/ProjectInstances';
import Project from '@/pages/automation/project/Project';
import Projects from '@/pages/automation/projects/Projects';
import {WorkflowExecutions as AutomationWorkflowExecutions} from '@/pages/automation/workflow-executions/WorkflowExecutions';
import OAuthPopup from '@/pages/platform/connection/components/oauth2/OAuthPopup';
import Account from '@/pages/settings/Account';
import Appearance from '@/pages/settings/Appearance';
import Settings from '@/pages/settings/Settings';
import Workspaces from '@/pages/settings/automation/workspaces/Workspaces';
import ErrorPage from '@/shared/error/ErrorPage';
import PageNotFound from '@/shared/error/PageNotFound';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter, redirect} from 'react-router-dom';

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
                errorElement: <ErrorPage />,
                path: 'automation',
            },
            {
                element: <PageNotFound />,
                path: '*',
            },
        ],
        element: <App />,
        errorElement: <ErrorPage />,
        path: '/',
    },
]);
