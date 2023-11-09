import {ComponentDefinitionModel} from '@/middleware/helios/execution/models';

export type DataPillType = {
    componentDefinition?: ComponentDefinitionModel | string;
    componentIcon?: string;
    id: string;
    value: string;
};

export type ComponentDataType = {
    action: string;
    name: string;
    connection?: object;
    notes?: string;
    properties?: object;
    title?: string;
    version?: number;
    workflowAlias?: string;
};
