import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {loader} from '@monaco-editor/react';
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
import YamlWorker from 'monaco-yaml/yaml.worker?worker';
import {Suspense, lazy, useEffect, useState} from 'react';

import type {EditorOptionsType, StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

// Worker constructors are tiny Vite-generated wrappers — safe to import statically.
// The actual worker scripts are code-split and only fetched when instantiated.
window.MonacoEnvironment = {
    getWorker(_moduleId: string, label: string) {
        switch (label) {
            case 'editorWorkerService':
                return new EditorWorker();
            case 'javascript':
            case 'typescript':
                return new TsWorker();
            case 'json':
                return new JsonWorker();
            case 'yaml':
                return new YamlWorker();
            default:
                console.error(
                    `MonacoEnvironment.getWorker: unexpected worker label "${label}", falling back to EditorWorker.`
                );

                return new EditorWorker();
        }
    },
};

// Lazily load the Monaco core library so that importing this module
// does not pull in the full ~2 MB Monaco bundle at parse time.
let monacoConfigured = false;

async function ensureMonacoConfigured(): Promise<void> {
    if (monacoConfigured) {
        return;
    }

    const monaco = await import('monaco-editor');

    loader.config({monaco});

    monacoConfigured = true;
}

const Editor = lazy(() => import('@monaco-editor/react'));

interface MonacoEditorProps {
    className?: string;
    defaultLanguage: string;
    onChange: (value: string | undefined) => void;
    onMount: (editor: StandaloneCodeEditorType) => void;
    options?: EditorOptionsType;
    value?: string;
}

const MonacoEditorWrapper = (props: MonacoEditorProps) => {
    const [isReady, setIsReady] = useState(monacoConfigured);

    useEffect(() => {
        if (!isReady) {
            ensureMonacoConfigured().then(() => setIsReady(true));
        }
    }, [isReady]);

    if (!isReady) {
        return <MonacoEditorLoader />;
    }

    return (
        <Suspense fallback={<MonacoEditorLoader />}>
            <Editor {...props} loading={<MonacoEditorLoader />} />
        </Suspense>
    );
};

export default MonacoEditorWrapper;
