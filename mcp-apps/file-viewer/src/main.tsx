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

// Map the text mime types getAssetFileContent returns to a highlight.js language; unknown -> auto-detect.
const MIME_TO_LANGUAGE: Record<string, string> = {
    'application/json': 'json',
    'application/xml': 'xml',
    'text/css': 'css',
    'text/csv': 'plaintext',
    'text/html': 'xml',
    'text/javascript': 'javascript',
    'text/markdown': 'markdown',
    'text/plain': 'plaintext',
    'text/x-python': 'python',
    'text/xml': 'xml',
    'text/yaml': 'yaml',
};

const FIXTURE_DATA: CodeDataI = {
    language: 'json',
    name: 'sample.json',
    source: '{\n    "hello": "world"\n}\n',
};

const fixtureMode =
    new URLSearchParams(window.location.search).has('fixture') || (import.meta.env.DEV && window.parent === window);

// getAssetFileContent results are shaped into structuredContent.{name, mimeType, content}.
function extractData(structuredContent: unknown): CodeDataI | undefined {
    if (!structuredContent || typeof structuredContent !== 'object') {
        return undefined;
    }

    const content = (structuredContent as Record<string, unknown>).content;

    if (typeof content !== 'string') {
        return undefined;
    }

    const name = (structuredContent as Record<string, unknown>).name;
    const mimeType = (structuredContent as Record<string, unknown>).mimeType;

    return {
        language: typeof mimeType === 'string' ? MIME_TO_LANGUAGE[mimeType] : undefined,
        name: typeof name === 'string' ? name : undefined,
        source: content,
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

        const app = new App({name: 'ByteChef File Viewer', version: '0.1.0'}, {});

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
                {status === 'connecting' ? 'Connecting to the host…' : 'Waiting for a file…'}
            </CenteredMessage>
        );
    }

    return <CodeView data={data} />;
}

const rootElement = document.getElementById('root');

if (rootElement) {
    createRoot(rootElement).render(<WidgetRoot />);
}
