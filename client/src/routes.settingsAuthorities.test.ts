import {getRouter} from '@/routes';
import {AUTHORITIES} from '@/shared/constants';
import {QueryClient} from '@tanstack/react-query';
import {ReactElement} from 'react';
import {RouteObject} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    createBrowserRouter: (routes: RouteObject[]) => ({routes}),
}));

type PrivateRouteElementType = ReactElement<{hasAnyAuthorities?: string[]}>;

const findRoutesByPath = (routes: RouteObject[], path: string): RouteObject[] =>
    routes.flatMap((route) => [
        ...(route.path === path ? [route] : []),
        ...(route.children ? findRoutesByPath(route.children, path) : []),
    ]);

const getRouteAuthorities = (path: string) => {
    const router = getRouter(new QueryClient());

    const routes = findRoutesByPath(router.routes as RouteObject[], path);

    return routes.map((route) => (route.element as PrivateRouteElementType).props.hasAnyAuthorities);
};

describe('settings route authorities', () => {
    it('lets only a tenant admin open the Billing settings page', () => {
        const authorities = getRouteAuthorities('billing');

        expect(authorities.length).toBeGreaterThan(0);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });

    it('lets only a tenant admin open the MCP Server settings page', () => {
        const authorities = getRouteAuthorities('mcp-server');

        expect(authorities.length).toBeGreaterThan(0);
        authorities.forEach((routeAuthorities) => expect(routeAuthorities).toEqual([AUTHORITIES.ADMIN]));
    });
});
