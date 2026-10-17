import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {CompletionContextType, ScriptLanguageType} from '@/shared/components/monaco/completion/completionTypes';
import {useCompletionContext} from '@/shared/components/monaco/completion/useCompletionContext';
import {useComponentCatalog} from '@/shared/components/monaco/completion/useComponentCatalog';
import {Suspense, lazy, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import type {editor} from 'monaco-editor';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface PropertyCodeEditorDialogEditorProps {
    language: string;
}

const PropertyCodeEditorDialogEditor = ({language}: PropertyCodeEditorDialogEditorProps) => {
    const [model, setModel] = useState<editor.ITextModel>();

    const {componentConnections, editorValue, inputParameters, scriptInput, setEditorValue} =
        usePropertyCodeEditorDialogStore(
            useShallow((state) => ({
                componentConnections: state.componentConnections,
                editorValue: state.editorValue,
                inputParameters: state.inputParameters,
                scriptInput: state.scriptInput,
                setEditorValue: state.setEditorValue,
            }))
        );

    const components = useComponentCatalog();

    const completionContext = useMemo<CompletionContextType | undefined>(() => {
        const scriptLanguage = language.toLowerCase();

        if (scriptLanguage !== 'javascript' && scriptLanguage !== 'python') {
            return undefined;
        }

        return {
            components,
            connections: componentConnections,
            input: inputParameters ?? scriptInput,
            kind: 'script',
            language: scriptLanguage as ScriptLanguageType,
        };
    }, [componentConnections, components, inputParameters, language, scriptInput]);

    useCompletionContext(model, completionContext);

    return (
        <Suspense fallback={<MonacoEditorLoader />}>
            <MonacoEditor
                className="size-full py-3"
                defaultLanguage={language}
                onChange={(value) => setEditorValue(value)}
                onMount={(mountedEditor) => {
                    mountedEditor.focus();

                    setModel(mountedEditor.getModel() ?? undefined);
                }}
                value={editorValue}
            />
        </Suspense>
    );
};

export default PropertyCodeEditorDialogEditor;
