import {App} from '@modelcontextprotocol/ext-apps';
import {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';

import './app.css';

import {SAMPLE_WORKFLOW_DEFINITION} from './fixtures/sampleWorkflow';
import {WorkflowDefinitionType} from './types';
import WorkflowGraph from './workflow-graph/WorkflowGraph';

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

// Fixture mode renders a bundled sample workflow instead of connecting to a host: explicit
// via ?fixture, implicit when the dev server page is opened directly (no embedding host).
const fixtureMode =
    new URLSearchParams(window.location.search).has('fixture') || (import.meta.env.DEV && window.parent === window);

// The workflow tools return the NESTED definition under structuredContent.definition —
// either as an object or as a JSON string.
function extractWorkflowDefinition(structuredContent: unknown): WorkflowDefinitionType | undefined {
    if (!structuredContent || typeof structuredContent !== 'object') {
        return undefined;
    }

    const definition = (structuredContent as Record<string, unknown>).definition;

    if (typeof definition === 'string') {
        try {
            return JSON.parse(definition) as WorkflowDefinitionType;
        } catch {
            return undefined;
        }
    }

    if (definition && typeof definition === 'object') {
        return definition as WorkflowDefinitionType;
    }

    return undefined;
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
    const [workflowDefinition, setWorkflowDefinition] = useState<WorkflowDefinitionType | undefined>(
        fixtureMode ? SAMPLE_WORKFLOW_DEFINITION : undefined
    );

    useEffect(() => {
        const handleToolResult = (result: ToolResultLikeI) => {
            const definition = extractWorkflowDefinition(result.structuredContent);

            if (definition) {
                setWorkflowDefinition(definition);
                setStatus('ready');
            }
        };

        window.__pushToolResult = handleToolResult;

        if (fixtureMode) {
            return;
        }

        const app = new App({name: 'ByteChef Workflow Viewer', version: '0.1.0'}, {});

        // Register before connect() so no notification is missed; the host replays the
        // triggering tool's result to the widget right after the ui/initialize handshake.
        app.ontoolresult = handleToolResult;

        app.connect()
            .then(() => setStatus((currentStatus) => (currentStatus === 'connecting' ? 'waiting' : currentStatus)))
            .catch(() => setStatus('error'));
    }, []);

    if (status === 'error') {
        return <CenteredMessage>Could not connect to the MCP host.</CenteredMessage>;
    }

    if (!workflowDefinition) {
        return (
            <CenteredMessage>
                {status === 'connecting' ? 'Connecting to the host…' : 'Waiting for a workflow…'}
            </CenteredMessage>
        );
    }

    return <WorkflowGraph workflowDefinition={workflowDefinition} />;
}

const rootElement = document.getElementById('root');

if (rootElement) {
    // No StrictMode: its double-invoked dev effects would open two host connections.
    createRoot(rootElement).render(<WidgetRoot />);
}
