import {createFileRoute} from '@tanstack/react-router';

export const Route = createFileRoute('/api/me')({
    server: {
        handlers: {
            GET: async ({context}) => {
                const connections = await context.bytechef.connections.list();

                return Response.json({connections: connections.length, login: context.user?.login ?? null});
            },
        },
    },
});
