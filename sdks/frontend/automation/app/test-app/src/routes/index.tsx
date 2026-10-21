import {toAiSdkTools} from '@bytechef/app/ai-sdk';
import {bytechefMiddleware} from '@bytechef/app/tanstack-start';
import {getWorkflowRun, listConnections} from '@bytechef/app/tanstack-start/server-fns';
import {createFileRoute} from '@tanstack/react-router';
import {createServerFn} from '@tanstack/react-start';
import {useState} from 'react';

const appMiddleware = bytechefMiddleware();

const countConnections = createServerFn({method: 'GET'})
    .middleware([appMiddleware])
    .handler(async ({context}) => (await context.bytechef.connections.list()).length);

const listAiToolNames = createServerFn({method: 'GET'})
    .middleware([appMiddleware])
    .handler(async ({context}) => Object.keys(await toAiSdkTools(context.bytechef, {workflows: true})));

const probes = [
    ['appServerFn', () => countConnections()],
    ['readyMadeServerFn', () => listConnections()],
    ['validatedServerFn', () => getWorkflowRun({data: {runId: 'run-1'}})],
    ['aiSdkTools', () => listAiToolNames()],
] as const;

async function runProbes(): Promise<string[]> {
    const results: string[] = [];

    for (const [label, probe] of probes) {
        try {
            results.push(`${label}=${JSON.stringify(await probe())}`);
        } catch (error) {
            const code = (error as {code?: unknown}).code;

            results.push(`${label}=ERROR ${typeof code === 'string' ? code : (error as Error).message}`);
        }
    }

    return results;
}

export const Route = createFileRoute('/')({
    component: Home,
    loader: () => runProbes(),
});

function Home() {
    const ssrResults = Route.useLoaderData();
    const [browserResults, setBrowserResults] = useState<string[]>([]);

    return (
        <main>
            <h1>@bytechef/app test app</h1>
            <pre id="ssr">{ssrResults.join('\n')}</pre>
            <button id="run" onClick={() => runProbes().then(setBrowserResults)} type="button">
                Run from the browser
            </button>
            <pre id="browser">{browserResults.join('\n')}</pre>
        </main>
    );
}
