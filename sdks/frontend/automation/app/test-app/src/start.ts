import {bytechefRequestMiddleware} from '@bytechef/app/tanstack-start';
import {createStart} from '@tanstack/react-start';

export const startInstance = createStart(() => ({
    requestMiddleware: bytechefRequestMiddleware(),
}));
