import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';

interface DevelopmentOnlyRouteI {
    fallbackHref: string;
    href: string;
}

const DEVELOPMENT_ONLY_ROUTES: DevelopmentOnlyRouteI[] = [
    {fallbackHref: '/automation/deployments', href: '/automation/projects'},
    {fallbackHref: '/embedded/configurations', href: '/embedded/integrations'},
    {fallbackHref: '/embedded/automation-configurations', href: '/embedded/automation-workflows'},
];

export function isDevelopmentOnlyHref(href: string): boolean {
    return DEVELOPMENT_ONLY_ROUTES.some((route) => route.href === href);
}

export function getDevelopmentOnlyFallbackHref(pathname: string, environmentId: number): string | undefined {
    if (environmentId === DEVELOPMENT_ENVIRONMENT) {
        return undefined;
    }

    const matchedRoute = DEVELOPMENT_ONLY_ROUTES.find(
        (route) => pathname === route.href || pathname.startsWith(`${route.href}/`)
    );

    return matchedRoute?.fallbackHref;
}
