import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import {isExternalTaskRunner} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/utils/isExternalTaskRunner';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {resolveMainClusterRootName} from '@/pages/platform/workflow-editor/utils/resolveClusterRootId';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {CompletionContextType, ScriptLanguageType} from '@/shared/components/monaco/completion/completionTypes';
import {useCompletionContext} from '@/shared/components/monaco/completion/useCompletionContext';
import {useComponentCatalog} from '@/shared/components/monaco/completion/useComponentCatalog';
import {useOperationPropertyLoaders} from '@/shared/components/monaco/completion/useOperationPropertyLoaders';
import {PlatformType} from '@/shared/middleware/graphql';
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
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const clusterElementType = currentNode?.clusterElementType;
    const currentNodeEnv = currentNode?.parameters?.env;
    const currentNodeName = currentNode?.name;
    const currentNodeTaskRunner = currentNode?.parameters?.taskRunner;
    const rootClusterElementNodeName = resolveMainClusterRootName(currentNode, rootClusterElementNodeData);

    const {includeComponents, platformType} = useWorkflowEditor();

    const components = useComponentCatalog({
        embedded: platformType === PlatformType.Embedded,
        include: includeComponents,
    });
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

        if (scriptLanguage === 'shell') {
            return {
                connections: componentConnections,
                env: currentNodeEnv && typeof currentNodeEnv === 'object' ? Object.keys(currentNodeEnv) : [],
                input: inputParameters ?? scriptInput,
                kind: 'shell',
            };
        }

        if (scriptLanguage !== 'javascript' && scriptLanguage !== 'python') {
            return undefined;
        }

        return {
            clusterElementNode,
            components,
            connections: componentConnections,
            externalTaskRunner: isExternalTaskRunner(scriptLanguage, {taskRunner: currentNodeTaskRunner}),
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
        currentNodeEnv,
        currentNodeTaskRunner,
        inputParameters,
        language,
        operationPropertyLoaders,
        rootClusterElementNodeName,
        scriptInput,
        workflowNodeName,
    ]);

    useCompletionContext(model, completionContext);
    const readOnly = useWorkflowEditorReadOnly();

    return (
        <Suspense fallback={<MonacoEditorLoader />}>
            <MonacoEditor
                className="size-full py-3"
                defaultLanguage={language}
                onChange={(value) => {
                    if (!readOnly) {
                        setEditorValue(value);
                    }
                }}
                onMount={(mountedEditor) => {
                    mountedEditor.focus();

                    setModel(mountedEditor.getModel() ?? undefined);
                }}
                options={{readOnly}}
                value={editorValue}
            />
        </Suspense>
    );
};

export default PropertyCodeEditorDialogEditor;
