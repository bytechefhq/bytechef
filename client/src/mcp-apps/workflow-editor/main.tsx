import {App} from '@modelcontextprotocol/ext-apps';
import {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';

import '@/styles/index.css';

import './workflowEditor.css';
import WorkflowGraph from './WorkflowGraph';
import {WorkflowDefinitionI} from './buildWidgetGraph';
import {SAMPLE_WORKFLOW_DEFINITION} from './fixtures/sampleWorkflow';

type ViewerStatusType = 'connecting' | 'waiting' | 'ready' | 'error';

interface ToolResultLikeI {
    structuredContent?: unknown;
}

const fixtureMode =
    new URLSearchParams(window.location.search).has('fixture') || (import.meta.env.DEV && window.parent === window);

function extractWorkflowDefinition(structuredContent: unknown): WorkflowDefinitionI | undefined {
    if (!structuredContent || typeof structuredContent !== 'object') {
        return undefined;
    }

    const definition = (structuredContent as Record<string, unknown>).definition;

    if (typeof definition === 'string') {
        try {
            return JSON.parse(definition) as WorkflowDefinitionI;
        } catch {
            return undefined;
        }
    }

    if (definition && typeof definition === 'object') {
        return definition as WorkflowDefinitionI;
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
    const [workflowDefinition, setWorkflowDefinition] = useState<WorkflowDefinitionI | undefined>(
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

        const app = new App({name: 'ByteChef Workflow Viewer', title: 'Workflow', version: '0.1.0'}, {});

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
    createRoot(rootElement).render(<WidgetRoot />);
}
