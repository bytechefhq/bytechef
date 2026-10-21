import {HeadContent, Outlet, Scripts, createRootRoute, useLocation} from '@tanstack/react-router';
import {TanStackRouterDevtoolsPanel} from '@tanstack/react-router-devtools';
import {TanStackDevtools} from '@tanstack/react-devtools';

import {Button} from '@/components/ui/button';
import {getSignIn} from '@/server/user';

import appCss from '../styles.css?url';

export const Route = createRootRoute({
    head: () => ({
        meta: [
            {
                charSet: 'utf-8',
            },
            {
                name: 'viewport',
                content: 'width=device-width, initial-scale=1',
            },
            {
                title: 'ByteChef app',
            },
        ],
        links: [
            {
                rel: 'stylesheet',
                href: appCss,
            },
        ],
    }),
    component: RootLayout,
    loader: () => getSignIn(),
    shellComponent: RootDocument,
});

function SignInBar() {
    const {enabled, user} = Route.useLoaderData();
    const location = useLocation();

    if (user) {
        return (
            <header className="flex items-center justify-end gap-3 border-b px-8 py-2 text-sm">
                <span id="signed-in-user">{user.name}</span>
                <form action="/auth/logout" method="post">
                    <Button id="sign-out" size="sm" type="submit" variant="outline">
                        Sign out
                    </Button>
                </form>
            </header>
        );
    }

    if (!enabled) {
        return null;
    }

    return (
        <header className="flex items-center justify-end border-b px-8 py-2 text-sm">
            <Button asChild size="sm" variant="outline">
                <a href={`/auth/login?returnTo=${encodeURIComponent(location.href)}`} id="sign-in">
                    Sign in
                </a>
            </Button>
        </header>
    );
}

function RootLayout() {
    return (
        <>
            <SignInBar />
            <Outlet />
        </>
    );
}

function RootDocument({children}: {children: React.ReactNode}) {
    return (
        <html lang="en">
            <head>
                <HeadContent />
            </head>
            <body>
                {children}
                {import.meta.env.DEV ? (
                    <TanStackDevtools
                        config={{
                            position: 'bottom-right',
                        }}
                        plugins={[
                            {
                                name: 'Tanstack Router',
                                render: <TanStackRouterDevtoolsPanel />,
                            },
                        ]}
                    />
                ) : null}
                <Scripts />
            </body>
        </html>
    );
}
