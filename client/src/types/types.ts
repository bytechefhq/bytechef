import {
    ArrayPropertyModel,
    BooleanPropertyModel,
    ComponentDefinitionBasicModel,
    ComponentDefinitionModel,
    ControlTypeModel,
    DatePropertyModel,
    DateTimePropertyModel,
    DynamicPropertiesPropertyModel,
    FileEntryPropertyModel,
    IntegerPropertyModel,
    NullPropertyModel,
    NumberPropertyModel,
    ObjectPropertyModel,
    PropertyModel,
    StringPropertyModel,
    TaskDispatcherDefinitionModel,
    TaskPropertyModel,
    TimePropertyModel,
    ValuePropertyModel,
} from '@/middleware/platform/configuration';

export type DataPillType = {
    componentName?: string;
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
    componentName: string;
    icon?: JSX.Element | string;
    notes?: string;
    parameters?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: any;
    };
    title?: string;
    workflowNodeName: string;
};

export type CurrentComponentDefinitionType = {
    workflowNodeName?: string;
} & ComponentDefinitionModel;

export type ClickedItemType = {
    componentName?: string;
} & (ComponentDefinitionBasicModel | TaskDispatcherDefinitionModel);

export type SubPropertyType = PropertyType & {custom: boolean};

export type WorkflowDefinitionType = {
    description?: string;
    label?: string;
    inputs?: Array<WorkflowInputType>;
    outputs?: Array<WorkflowOutputType>;
    tasks?: Array<WorkflowTaskType>;
    triggers?: Array<WorkflowTriggerType>;
};

export type WorkflowInputType = {
    label?: string;
    name: string;
    required?: boolean;
    type?: string;
};

export type WorkflowOutputType = {
    name: string;
    value: object;
};

export type WorkflowTaskType = {
    connections: [
        {
            componentName: string;
            componentVersion: number;
            key: string;
            required: boolean;
            workflowNodeName: string;
        },
    ] & {
        [key: string]: WorkflowConnectionType;
    };
    finalize?: Array<WorkflowTaskType>;
    label?: string;
    name: string;
    node?: string;
    parameters?: {[key: string]: object};
    post?: Array<WorkflowTaskType>;
    pre?: Array<WorkflowTaskType>;
    timeout?: string;
    type: string;
};

export type WorkflowTriggerType = {
    label?: string;
    name: string;
    parameters?: {[key: string]: object};
    timeout?: string;
    type: string;
};

export type WorkflowConnectionType = {
    componentName: string;
    componentVersion: number;
};

export type ArrayPropertyType = PropertyModel & {
    controlType?: ControlTypeModel;
    custom?: boolean;
    defaultValue?: string;
};

type PropertyTypeAllType = ArrayPropertyModel &
    BooleanPropertyModel &
    DatePropertyModel &
    DateTimePropertyModel &
    DynamicPropertiesPropertyModel &
    FileEntryPropertyModel &
    IntegerPropertyModel &
    NumberPropertyModel &
    NullPropertyModel &
    ObjectPropertyModel &
    PropertyModel &
    StringPropertyModel &
    TaskPropertyModel &
    TimePropertyModel &
    ValuePropertyModel;

export type PropertyType = Omit<PropertyTypeAllType, 'controlType'> & {
    controlType?: ControlTypeModel;
};
