import {createFileRoute, lazyRouteComponent} from '@tanstack/react-router';

import type {AppOverview} from '@/server/overview';

type LoaderData = {overview: AppOverview; placeholder: false} | {overview: null; placeholder: true};

async function loadOverview(): Promise<LoaderData> {
    if (!import.meta.env.DEV) {
        return {overview: null, placeholder: true};
    }

    const {getAppOverview} = await import('@/server/dev');

    return {overview: (await getAppOverview()) as AppOverview, placeholder: false};
}

function Placeholder() {
    return (
        <main className="mx-auto max-w-2xl p-8" id="placeholder">
            <h1 className="text-2xl font-semibold">This app is ready to be built</h1>
            <p className="mt-2 text-muted-foreground">
                Run it in development to see what it may call, then replace this page with your features.
            </p>
        </main>
    );
}

export const Route = createFileRoute('/')({
    component: import.meta.env.DEV ? lazyRouteComponent(() => import('@/components/overview'), 'DevHome') : Placeholder,
    loader: loadOverview,
});
