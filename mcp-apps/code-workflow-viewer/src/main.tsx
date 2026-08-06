import {App} from '@modelcontextprotocol/ext-apps';
import {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';

import './app.css';

import CodeView, {CodeDataI} from './CodeView';

type ViewerStatusType = 'connecting' | 'waiting' | 'ready' | 'error';

interface ToolResultLikeI {
    structuredContent?: unknown;
}

declare global {
    interface Window {
        __pushToolResult?: (result: ToolResultLikeI) => void;
    }
}

// A real single-file code workflow, so the standalone dev render shows the contract someone will
// actually see: a completion value exposing members, tasks whose perform takes a context, and a
// group for the work that runs concurrently.
const FIXTURE_DATA: CodeDataI = {
    language: 'javascript',
    name: 'sample-workflow.js',
    source: `({
    name: "sample-workflow",
    version: "1",
    workflows: [
        {
            name: "orders",
            label: "Orders",
            tasks: [
                {
                    name: "fetch-order",
                    connections: [{componentName: "httpClient", name: "orders-api"}],
                    perform: function (context) {
                        return context.component.httpClient.get(
                            {uri: "https://api.example.com/orders/" + context.input().input.orderId},
                            "orders-api");
                    }
                },
                {
                    name: "enrich",
                    type: "parallel",
                    tasks: [
                        {name: "fetch-customer", perform: (context) => context.input("fetch-order").body.customer},
                        {name: "fetch-inventory", perform: (context) => context.input("fetch-order").body.items}
                    ]
                }
            ]
        }
    ]
})
`,
};

const fixtureMode =
    new URLSearchParams(window.location.search).has('fixture') || (import.meta.env.DEV && window.parent === window);

// getCodeWorkflowSource results are shaped into structuredContent.source (+ optional name/language).
function extractData(structuredContent: unknown): CodeDataI | undefined {
    if (!structuredContent || typeof structuredContent !== 'object') {
        return undefined;
    }

    const source = (structuredContent as Record<string, unknown>).source;

    if (typeof source !== 'string') {
        return undefined;
    }

    const name = (structuredContent as Record<string, unknown>).name;
    const language = (structuredContent as Record<string, unknown>).language;

    return {
        language: typeof language === 'string' ? language : undefined,
        name: typeof name === 'string' ? name : undefined,
        source,
    };
}

function CenteredMessage({children}: {children: string}) {
    return (
        <div className="flex h-full items-center justify-center bg-surface-neutral-primary text-sm text-content-neutral-secondary">
            {children}
        </div>
    );
}

function WidgetRoot() {
    const [status, setStatus] = useState<ViewerStatusType>(fixtureMode ? 'ready' : 'connecting');
    const [data, setData] = useState<CodeDataI | undefined>(fixtureMode ? FIXTURE_DATA : undefined);

    useEffect(() => {
        const handleToolResult = (result: ToolResultLikeI) => {
            const extracted = extractData(result.structuredContent);

            if (extracted) {
                setData(extracted);
                setStatus('ready');
            }
        };

        window.__pushToolResult = handleToolResult;

        if (fixtureMode) {
            return;
        }

        const app = new App({name: 'ByteChef Code Workflow Viewer', version: '0.1.0'}, {});

        app.ontoolresult = handleToolResult;

        app.connect()
            .then(() => setStatus((currentStatus) => (currentStatus === 'connecting' ? 'waiting' : currentStatus)))
            .catch(() => setStatus('error'));
    }, []);

    if (status === 'error') {
        return <CenteredMessage>Could not connect to the MCP host.</CenteredMessage>;
    }

    if (!data) {
        return (
            <CenteredMessage>
                {status === 'connecting' ? 'Connecting to the host…' : 'Waiting for source…'}
            </CenteredMessage>
        );
    }

    return <CodeView data={data} />;
}

const rootElement = document.getElementById('root');

if (rootElement) {
    createRoot(rootElement).render(<WidgetRoot />);
}
