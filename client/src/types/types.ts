import {
    ComponentDefinitionBasicModel,
    ComponentDefinitionModel,
    ConnectionDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '@/middleware/platform/configuration';

export type DataPillType = {
    componentAlias?: string;
    componentDefinition?: ComponentDefinitionModel | string;
    componentIcon?: string;
    id: string;
    value: string;
};

export type ComponentActionType = {
    actionName: string;
    componentName: string;
    workflowNodeName?: string;
};

export type ComponentDataType = {
    actionName: string;
    connection?: object;
    componentName: string;
    icon?: JSX.Element | string;
    notes?: string;
    parameters?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: any;
    };
    title?: string;
    type?: string;
    version?: number;
    workflowNodeName: string;
};

export type CurrentComponentType = {
    connection?: ConnectionDefinitionBasicModel;
    notes?: string;
    parameters?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: any;
    };
    workflowNodeName?: string;
} & ComponentDefinitionModel;

export type ClickedItemType = {
    componentName?: string;
} & (ComponentDefinitionBasicModel | TaskDispatcherDefinitionModel);

export interface WorkflowDefinition {
    description?: string;
    label?: string;
    inputs?: Array<WorkflowInput>;
    outputs?: Array<WorkflowOutput>;
    tasks?: Array<WorkflowTask>;
    triggers?: Array<WorkflowTrigger>;
}

export interface WorkflowInput {
    label?: string;
    name: string;
    required?: boolean;
    type?: string;
}

export interface WorkflowOutput {
    name: string;
    value: object;
}

export interface WorkflowTask {
    finalize?: Array<WorkflowTask>;
    label?: string;
    name: string;
    node?: string;
    parameters?: {[key: string]: object};
    post?: Array<WorkflowTask>;
    pre?: Array<WorkflowTask>;
    timeout?: string;
    type: string;
}

export interface WorkflowTrigger {
    label?: string;
    name: string;
    parameters?: {[key: string]: object};
    timeout?: string;
    type: string;
}
