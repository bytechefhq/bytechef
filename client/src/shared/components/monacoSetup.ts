import {defineMonacoDarkTheme} from '@/shared/util/monacoTheme-utils';
import {type Monaco, loader} from '@monaco-editor/react';
import EditorWorker from 'monaco-editor/editor/editor.worker.js?worker';
import JsonWorker from 'monaco-editor/language/json/json.worker.js?worker';
import TsWorker from 'monaco-editor/language/typescript/ts.worker.js?worker';
import YamlWorker from 'monaco-yaml/yaml.worker?worker';

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

let monacoConfigured = false;
let monacoInstance: Monaco | undefined;

export async function ensureMonacoConfigured(): Promise<void> {
    if (monacoConfigured) {
        return;
    }

    const monaco = await import('monaco-editor');

    loader.config({monaco});

    defineMonacoDarkTheme(monaco, false);

    monacoInstance = monaco;
    monacoConfigured = true;
}

export function isMonacoConfigured(): boolean {
    return monacoConfigured;
}

export function getMonacoInstance(): Monaco | undefined {
    return monacoInstance;
}
