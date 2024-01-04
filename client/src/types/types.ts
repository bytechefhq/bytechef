import {ComponentDefinitionModel} from '@/middleware/hermes/configuration';

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
    action?: string;
    name: string;
    connection?: object;
    notes?: string;
    properties?: {
        [key: string]: {
            [key: string]: string;
        };
    };
    title?: string;
    type?: string;
    version?: number;
    workflowNodeName?: string;
};

export type CurrentComponentType = {
    connection?: object;
    notes?: string;
    properties?: object;
    workflowNodeName?: string;
} & ComponentDefinitionModel;

interface WorkflowDefinitionModel {
    action?: string;
    name?: string;
    components?: Array<ComponentDataType>;
    flowControls?: Array<ComponentDataType>;
    tasks?: Array<ComponentDataType>;
}

export type WorkflowDefinitionType = {
    [workflowId: string]: WorkflowDefinitionModel;
};
