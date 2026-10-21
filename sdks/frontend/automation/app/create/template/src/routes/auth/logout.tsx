import {logoutHandler} from '@bytechef/app/tanstack-start';
import {createFileRoute} from '@tanstack/react-router';

import {authOptions} from '@/server/bytechef';

export const Route = createFileRoute('/auth/logout')({
    server: {handlers: {POST: logoutHandler(authOptions)}},
});
