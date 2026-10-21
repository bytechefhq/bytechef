import {loginHandler} from '@bytechef/app/tanstack-start';
import {createFileRoute} from '@tanstack/react-router';

export const Route = createFileRoute('/auth/login')({
    server: {handlers: {GET: loginHandler()}},
});
