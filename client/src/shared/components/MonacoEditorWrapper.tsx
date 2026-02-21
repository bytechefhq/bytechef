import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {loader} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
import YamlWorker from 'monaco-yaml/yaml.worker?worker';
import {Suspense, lazy} from 'react';

import type {EditorOptionsType, StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

// Use the locally installed Monaco instead of loading from CDN,
// ensuring the editor and its web workers share the same version.
loader.config({monaco});

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
                return new EditorWorker();
        }
    },
};

const Editor = lazy(() => import('@monaco-editor/react'));

interface MonacoEditorProps {
    className?: string;
    defaultLanguage: string;
    onChange: (value: string | undefined) => void;
    onMount: (editor: StandaloneCodeEditorType) => void;
    options?: EditorOptionsType;
    value?: string;
}

const MonacoEditorWrapper = (props: MonacoEditorProps) => (
    <Suspense fallback={<MonacoEditorLoader />}>
        <Editor {...props} loading={<MonacoEditorLoader />} />
    </Suspense>
);

export default MonacoEditorWrapper;
