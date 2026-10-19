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
    getDisplayConditions(
        componentName: string,
        componentVersion: number | undefined,
        operationName: string,
        operationKind: OperationKindType,
        parameters: Record<string, unknown>
    ): Promise<Record<string, boolean>>;
    getOperationProperties(
        componentName: string,
        componentVersion: number | undefined,
        operationName: string,
        operationKind: OperationKindType
    ): Promise<Property[]>;
};

export type OperationKindType = 'action' | 'trigger';

export type DynamicPropertiesRequestType = {
    actionName: string;
    componentName: string;
    componentVersion: number;
    connectionKey?: string;
    lookupDependsOnPaths: string[];
    parameters: Record<string, unknown>;
    propertyName: string;
    workflowNodeName: string;
};

export type DynamicPropertiesLoaderType = (request: DynamicPropertiesRequestType) => Promise<Property[]>;

export interface ScriptCompletionContextI {
    components: ComponentCatalogType;
    connections: ConnectionDeclarationType[];
    input: Record<string, unknown>;
    kind: 'script';
    language: ScriptLanguageType;
    loadDynamicProperties?: DynamicPropertiesLoaderType;
    workflowNodeName?: string;
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
    loadDynamicProperties?: DynamicPropertiesLoaderType;
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
