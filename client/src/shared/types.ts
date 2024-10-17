import {UpdateWorkflowRequestI} from '@/shared//mutations/platform/workflows.mutations';
import {
    ArrayProperty,
    BooleanProperty,
    ComponentDefinition,
    ComponentDefinitionBasic,
    ControlType,
    DateProperty,
    DateTimeProperty,
    DynamicPropertiesProperty,
    FileEntryProperty,
    IntegerProperty,
    NullProperty,
    NumberProperty,
    ObjectProperty,
    Property,
    StringProperty,
    TaskDispatcherDefinition,
    TaskProperty,
    TimeProperty,
    TriggerDefinition,
    ValueProperty,
    Workflow,
    WorkflowConnection,
} from '@/shared/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';
import {ReactNode} from 'react';

export type DataPillType = {
    componentName?: string;
    componentDefinition?: ComponentDefinition | string;
    componentIcon?: string;
    id: string;
    nodeName?: string;
    value: string;
};

export type ComponentOperationType = {
    componentName: string;
    operationName: string;
    workflowNodeName?: string;
};

export type ComponentPropertiesType =
    | {
          componentDefinition: ComponentDefinitionBasic;
          properties?: Array<Property>;
      }
    | undefined;

export type TaskDispatcherType = {
    componentName: string;
    icon: ReactNode;
    label: string;
    name: string;
};

export type ComponentType = {
    componentName: string;
    description?: string;
    displayConditions?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: boolean;
    };
    metadata?: {
        ui?: {
            condition?: string;
            dynamicPropertyTypes?: {[key: string]: string};
        };
    };
    notes?: string;
    operationName?: string;
    parameters?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: any;
    };
    title?: string;
    type?: string;
    workflowNodeName: string;
};

export type ClickedDefinitionType = {
    taskDispatcher?: boolean;
    trigger?: boolean;
} & (ComponentDefinition & TriggerDefinition & TaskDispatcherDefinition);

export type ClickedOperationType = {
    componentLabel?: string;
    componentName: string;
    icon?: string;
    operationName: string;
    taskDispatcher?: boolean;
    trigger?: boolean;
    type: string;
    version: number;
};

export type NodeDataType = {
    componentName: string;
    connections?: Array<WorkflowConnection>;
    description?: string;
    icon?: JSX.Element | ReactNode | string;
    label?: string;
    metadata?: {
        ui?: {
            condition?: string;
            dynamicPropertyTypes?: {[key: string]: string};
        };
    };
    name: string;
    operationName?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    taskDispatcher?: boolean;
    trigger?: boolean;
    type?: string;
    version?: number;
    workflowNodeName?: string;
};

export type NodeType = {
    componentName?: string;
    connections?: Array<WorkflowConnectionType>;
    connectionId?: number;
    displayConditions?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: boolean;
    };
    metadata?: {
        ui?: {
            condition?: string;
            dynamicPropertyTypes?: {[key: string]: string};
        };
    };
    icon?: ReactNode;
    id: string;
    label?: string;
    name: string;
    operationName?: string;
    parameters?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: any;
    };
    taskDispatcher?: boolean;
    trigger?: boolean;
    type: string;
    version: number;
    workflowNodeName?: string;
};

export type SubPropertyType = PropertyAllType & {custom: boolean};

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

export type ArrayPropertyType = Property & {
    additionalProperties?: Array<Property>;
    controlType?: ControlType;
    custom?: boolean;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    defaultValue?: any;
    key?: string;
    label?: string;
    properties?: Array<PropertyAllType>;
};

type PropertyTypeAllType = ArrayProperty &
    BooleanProperty &
    DateProperty &
    DateTimeProperty &
    DynamicPropertiesProperty &
    FileEntryProperty &
    IntegerProperty &
    NumberProperty &
    NullProperty &
    ObjectProperty &
    Property &
    StringProperty &
    TaskProperty &
    TimeProperty &
    ValueProperty;

export type PropertyAllType = Omit<PropertyTypeAllType, 'controlType'> & {
    additionalProperties?: Array<Property>;
    controlType?: ControlType;
    custom?: boolean;
    expressionEnabled?: boolean;
};

export type UpdateWorkflowMutationType = UseMutationResult<Workflow, Error, UpdateWorkflowRequestI, unknown>;
