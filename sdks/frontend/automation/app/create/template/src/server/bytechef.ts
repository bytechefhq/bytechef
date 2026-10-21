import {bytechefRequestMiddleware, type BytechefAuthOptionsI} from '@bytechef/app/tanstack-start';
import {createIsomorphicFn} from '@tanstack/react-start';

interface ServerGlobalI {
    process?: {env: Record<string, string | undefined>};
}

const readAppUrl = createIsomorphicFn()
    .server(() => (globalThis as ServerGlobalI).process?.env.APP_URL || undefined)
    .client(() => undefined);

export const authOptions: BytechefAuthOptionsI = {appUrl: readAppUrl()};

export const requestMiddleware = bytechefRequestMiddleware(authOptions);

export const bytechef = requestMiddleware[1];
