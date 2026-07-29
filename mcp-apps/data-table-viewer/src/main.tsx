import {App} from '@modelcontextprotocol/ext-apps';
import {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';

import './app.css';

import DataTableView, {DataTableDataI} from './DataTableView';

type ViewerStatusType = 'connecting' | 'waiting' | 'ready' | 'error';

interface ToolResultLikeI {
    structuredContent?: unknown;
}

declare global {
    interface Window {
        // Dev/test hook: push a CallToolResult-shaped object to simulate a host-delivered
        // tool result (same code path as app.ontoolresult).
        __pushToolResult?: (result: ToolResultLikeI) => void;
    }
}

const FIXTURE_DATA: DataTableDataI = {
    name: 'Sample table',
    rows: [
        {email: 'ada@example.com', id: 1, name: 'Ada Lovelace'},
        {email: 'alan@example.com', id: 2, name: 'Alan Turing'},
    ],
};

// Fixture mode renders bundled sample data instead of connecting to a host: explicit via
// ?fixture, implicit when the dev server page is opened directly (no embedding host).
const fixtureMode =
    new URLSearchParams(window.location.search).has('fixture') || (import.meta.env.DEV && window.parent === window);

// queryDataTable results are shaped into structuredContent.rows (+ optional name) by the server.
function extractData(structuredContent: unknown): DataTableDataI | undefined {
    if (!structuredContent || typeof structuredContent !== 'object') {
        return undefined;
    }

    const rows = (structuredContent as Record<string, unknown>).rows;

    if (!Array.isArray(rows)) {
        return undefined;
    }

    const name = (structuredContent as Record<string, unknown>).name;

    return {name: typeof name === 'string' ? name : undefined, rows: rows as Record<string, unknown>[]};
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
    const [data, setData] = useState<DataTableDataI | undefined>(fixtureMode ? FIXTURE_DATA : undefined);

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

        const app = new App({name: 'ByteChef Data Table Viewer', version: '0.1.0'}, {});

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
                {status === 'connecting' ? 'Connecting to the host…' : 'Waiting for a data table…'}
            </CenteredMessage>
        );
    }

    return <DataTableView data={data} />;
}

const rootElement = document.getElementById('root');

if (rootElement) {
    // No StrictMode: its double-invoked dev effects would open two host connections.
    createRoot(rootElement).render(<WidgetRoot />);
}
