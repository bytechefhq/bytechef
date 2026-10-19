import type {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import type {
    ComponentDefinition,
    ComponentDefinitionBasic,
    ConnectionDefinition,
    Property,
    WorkflowNodeOutput,
} from '@/shared/middleware/platform/configuration';

export type ScriptLanguageType = 'javascript' | 'python' | 'ruby';

export type ConnectionDeclarationType = {
    componentName: string;
    componentVersion: number;
    name: string;
};

export type ComponentCatalogType = {
    components: ComponentDefinitionBasic[];
    getConnectionDefinition(
        componentName: string,
        componentVersion?: number
    ): Promise<ConnectionDefinition | undefined>;
    getDefinition(componentName: string, componentVersion?: number): Promise<ComponentDefinition | undefined>;
    getOperationProperties(
        componentName: string,
        componentVersion: number | undefined,
        operationName: string,
        operationKind: OperationKindType
    ): Promise<Property[]>;
};

export type OperationKindType = 'action' | 'trigger';

export interface ScriptCompletionContextI {
    components: ComponentCatalogType;
    connections: ConnectionDeclarationType[];
    input: Record<string, unknown>;
    kind: 'script';
    language: ScriptLanguageType;
}

export interface CustomComponentCompletionContextI {
    kind: 'customComponent';
    language: ScriptLanguageType;
}

export interface CodeWorkflowCompletionContextI {
    components: ComponentCatalogType;
    kind: 'codeWorkflow';
    language: ScriptLanguageType;
}

export interface WorkflowDefinitionCompletionContextI {
    components: ComponentCatalogType;
    format: 'json' | 'yaml';
    functions: EvaluatorFunctionDefinition[];
    getNodeOutputs: () => Promise<WorkflowNodeOutput[]>;
    kind: 'workflowDefinition';
}

export type CompletionContextType =
    | ScriptCompletionContextI
    | CustomComponentCompletionContextI
    | CodeWorkflowCompletionContextI
    | WorkflowDefinitionCompletionContextI;

export type PerformScopeType = {
    connectionName?: string;
    contextName?: string;
    inputName?: string;
    performStart: number;
};
