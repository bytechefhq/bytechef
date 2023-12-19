import {ComponentDefinitionModel} from '@/middleware/hermes/configuration';

export type DataPillType = {
    componentAlias?: string;
    componentDefinition?: ComponentDefinitionModel | string;
    componentIcon?: string;
    id: string;
    value: string;
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
    workflowAlias?: string;
};

export type CurrentComponentType = {
    connection?: object;
    notes?: string;
    properties?: object;
    workflowAlias?: string;
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
