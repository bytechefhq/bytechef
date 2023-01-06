import {createBrowserRouter} from 'react-router-dom';
import App from './App';
import ErrorPage from './ErrorPage';
import Integration from './pages/integration/Integration';
import Integrations from './pages/integrations/Integrations';
import {Connections} from './pages/connections/Connections';
import Settings from './pages/settings/Settings';

export const router = createBrowserRouter([
    {
        path: '/',
        element: <App />,
        errorElement: <ErrorPage />,
        children: [
            {
                path: '',
                element: <Integrations />,
            },
            {
                path: 'integration',
                element: <Integration />,
            },
            {
                path: 'integrations',
                element: <Integrations />,
            },
            {
                path: 'connections',
                element: <Connections />,
            },
            {
                path: 'settings',
                element: <Settings />,
            },
        ],
    },
]);
