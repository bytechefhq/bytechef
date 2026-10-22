import {getRouter} from '@/routes';
import {DEVELOPMENT_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryClient} from '@tanstack/react-query';
import {LoaderFunctionArgs, RouteObject} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    createBrowserRouter: (routes: RouteObject[]) => ({routes}),
}));

const PROJECT_WORKFLOW_EDITOR_PATH = 'projects/:projectId/project-workflows/:projectWorkflowId';
const INTEGRATION_WORKFLOW_EDITOR_PATH = 'integrations/:integrationId/integration-workflows/:integrationWorkflowId';

const findRoute = (routes: RouteObject[], parentPath: string, path: string): RouteObject | undefined => {
    for (const route of routes) {
        if (route.path === parentPath) {
            const childRoute = findChildRoute(route.children ?? [], path);

            if (childRoute) {
                return childRoute;
            }
        }

        const nestedRoute = findRoute(route.children ?? [], parentPath, path);

        if (nestedRoute) {
            return nestedRoute;
        }
    }

    return undefined;
};

const findChildRoute = (routes: RouteObject[], path: string): RouteObject | undefined => {
    for (const route of routes) {
        if (route.path === path) {
            return route;
        }

        const childRoute = findChildRoute(route.children ?? [], path);

        if (childRoute) {
            return childRoute;
        }
    }

    return undefined;
};

const callLoader = async (parentPath: string, path: string, params: Record<string, string> = {}) => {
    const route = findRoute(getRouter(queryClient).routes, parentPath, path);

    if (typeof route?.loader !== 'function') {
        throw new Error(`No loader for ${parentPath}/${path}`);
    }

    return route.loader({
        context: undefined,
        params,
        request: new Request('http://localhost/'),
    } as LoaderFunctionArgs);
};

let queryClient: QueryClient;

describe('development-only routes', () => {
    beforeEach(() => {
        queryClient = new QueryClient();

        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
    });

    it.each([
        ['automation', 'projects', {}, '/automation/deployments'],
        [
            'automation',
            PROJECT_WORKFLOW_EDITOR_PATH,
            {projectId: '7', projectWorkflowId: '8'},
            '/automation/deployments',
        ],
        ['embedded', 'integrations', {}, '/embedded/configurations'],
        [
            'embedded',
            INTEGRATION_WORKFLOW_EDITOR_PATH,
            {integrationId: '7', integrationWorkflowId: '8'},
            '/embedded/configurations',
        ],
    ])('redirects %s/%s outside the development environment', async (parentPath, path, params, location) => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        const querySpy = vi.spyOn(queryClient, 'query');

        const response = (await callLoader(parentPath, path, params)) as Response;

        expect(response.status).toBe(302);
        expect(response.headers.get('Location')).toBe(location);
        expect(querySpy).not.toHaveBeenCalled();
    });

    it.each([
        ['automation', 'projects'],
        ['embedded', 'integrations'],
    ])('opens %s/%s in the development environment', async (parentPath, path) => {
        expect(await callLoader(parentPath, path)).toBeNull();
    });

    it.each([
        ['automation', PROJECT_WORKFLOW_EDITOR_PATH, {projectId: '7', projectWorkflowId: '8'}],
        ['embedded', INTEGRATION_WORKFLOW_EDITOR_PATH, {integrationId: '7', integrationWorkflowId: '8'}],
    ])('loads the editor data of %s/%s in the development environment', async (parentPath, path, params) => {
        const data = {id: 7};

        const querySpy = vi.spyOn(queryClient, 'query').mockResolvedValue(data);

        expect(await callLoader(parentPath, path, params)).toBe(data);
        expect(querySpy).toHaveBeenCalledTimes(1);
    });
});
