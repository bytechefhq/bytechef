import {logoutHandler} from '@bytechef/app/tanstack-start';
import {createFileRoute} from '@tanstack/react-router';

export const Route = createFileRoute('/auth/logout')({
    server: {handlers: {POST: logoutHandler()}},
});
