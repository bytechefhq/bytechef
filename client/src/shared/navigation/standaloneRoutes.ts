export const STANDALONE_ROUTES = ['/automation/approval-tasks', '/automation/portal'];

export function isStandaloneRoute(pathname: string): boolean {
    return STANDALONE_ROUTES.some((route) => pathname === route || pathname.startsWith(`${route}/`));
}
