import {ComponentDefinitionModel} from '@/middleware/helios/execution/models';

export type DataPillType = {
    component?: ComponentDefinitionModel | string;
    icon?: string;
    id: string;
    value: string;
};
