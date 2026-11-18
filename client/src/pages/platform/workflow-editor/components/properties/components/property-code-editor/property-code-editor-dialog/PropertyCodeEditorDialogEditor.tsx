import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {CompletionContextType, ScriptLanguageType} from '@/shared/components/monaco/completion/completionTypes';
import {useCompletionContext} from '@/shared/components/monaco/completion/useCompletionContext';
import {useComponentCatalog} from '@/shared/components/monaco/completion/useComponentCatalog';
import {useDynamicPropertiesLoader} from '@/shared/components/monaco/completion/useDynamicPropertiesLoader';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Suspense, lazy, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import type {editor} from 'monaco-editor';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface PropertyCodeEditorDialogEditorProps {
    language: string;
    workflowId?: string;
    workflowNodeName?: string;
}

const PropertyCodeEditorDialogEditor = ({
    language,
    workflowId,
    workflowNodeName,
}: PropertyCodeEditorDialogEditorProps) => {
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
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const clusterElementType = useWorkflowNodeDetailsPanelStore((state) => state.currentNode?.clusterElementType);
    const rootClusterElementNodeName = useWorkflowEditorStore(
        (state) => state.rootClusterElementNodeData?.workflowNodeName
    );

    const components = useComponentCatalog();
    const loadDynamicProperties = useDynamicPropertiesLoader(workflowId, currentEnvironmentId);

    const isClusterElement = !!clusterElementType && !!rootClusterElementNodeName;

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
            ...(!isClusterElement && {loadDynamicProperties, workflowNodeName}),
        };
    }, [
        componentConnections,
        components,
        inputParameters,
        isClusterElement,
        language,
        loadDynamicProperties,
        scriptInput,
        workflowNodeName,
    ]);

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
