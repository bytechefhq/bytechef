import {callbackHandler} from '@bytechef/app/tanstack-start';
import {createFileRoute} from '@tanstack/react-router';

export const Route = createFileRoute('/auth/callback')({
    server: {handlers: {GET: callbackHandler()}},
});
