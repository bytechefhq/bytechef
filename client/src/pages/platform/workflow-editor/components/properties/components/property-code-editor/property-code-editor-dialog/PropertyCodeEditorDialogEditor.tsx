import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {CompletionContextType, ScriptLanguageType} from '@/shared/components/monaco/completion/completionTypes';
import {useCompletionContext} from '@/shared/components/monaco/completion/useCompletionContext';
import {useComponentCatalog} from '@/shared/components/monaco/completion/useComponentCatalog';
import {useOperationPropertyLoaders} from '@/shared/components/monaco/completion/useOperationPropertyLoaders';
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
    const {clusterElementType, currentNodeName} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            clusterElementType: state.currentNode?.clusterElementType,
            currentNodeName: state.currentNode?.name,
        }))
    );
    const rootClusterElementNodeName = useWorkflowEditorStore(
        (state) => state.rootClusterElementNodeData?.workflowNodeName
    );

    const components = useComponentCatalog();
    const operationPropertyLoaders = useOperationPropertyLoaders(workflowId, currentEnvironmentId);

    const clusterElementNode = useMemo(
        () =>
            clusterElementType && rootClusterElementNodeName && currentNodeName
                ? {clusterElementType, clusterElementWorkflowNodeName: currentNodeName}
                : undefined,
        [clusterElementType, currentNodeName, rootClusterElementNodeName]
    );

    const completionContext = useMemo<CompletionContextType | undefined>(() => {
        const scriptLanguage = language.toLowerCase();

        if (scriptLanguage !== 'javascript' && scriptLanguage !== 'python') {
            return undefined;
        }

        return {
            clusterElementNode,
            components,
            connections: componentConnections,
            input: inputParameters ?? scriptInput,
            kind: 'script',
            language: scriptLanguage as ScriptLanguageType,
            ...operationPropertyLoaders,
            workflowNodeName: clusterElementNode ? rootClusterElementNodeName : workflowNodeName,
        };
    }, [
        clusterElementNode,
        componentConnections,
        components,
        inputParameters,
        language,
        operationPropertyLoaders,
        rootClusterElementNodeName,
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
