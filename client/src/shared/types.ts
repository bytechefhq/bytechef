import {UpdateWorkflowRequestI} from '@/shared//mutations/platform/workflows.mutations';
import {
    ArrayProperty,
    BooleanProperty,
    ComponentConnection,
    ComponentDefinition,
    ComponentDefinitionBasic,
    ConnectionDefinitionBasic,
    ControlType,
    DateProperty,
    DateTimeProperty,
    DynamicPropertiesProperty,
    FileEntryProperty,
    IntegerProperty,
    // NullProperty,
    NumberProperty,
    ObjectProperty,
    Property,
    StringProperty,
    TaskDispatcherDefinition,
    TaskProperty,
    TimeProperty,
    TriggerDefinition,
    TriggerType,
    ValueProperty,
    WorkflowInput,
    WorkflowTask,
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

export type ConditionTaskDispatcherType = {
    parameters: {
        caseFalse: Array<WorkflowTaskType>;
        caseTrue: Array<WorkflowTaskType>;
        [key: string]: unknown;
    };
} & WorkflowTaskType;

export type TaskDispatcherType = {
    componentName: string;
    icon: ReactNode;
    label: string;
    name: string;
};

export type ComponentType = {
    componentName: string;
    connections?: Array<ComponentConnection>;
    connectionId?: number;
    description?: string;
    displayConditions?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: boolean;
    };
    label?: string;
    metadata?: {
        ui?: {
            condition?: string;
            dynamicPropertyTypes?: {[key: string]: string};
        };
    };
    name?: string;
    operationName?: string;
    parameters?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: any;
    };
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

export type TabNameType = 'description' | 'clusterElements' | 'connection' | 'properties' | 'output';

type ConditionDataType = {
    conditionCase: 'caseTrue' | 'caseFalse';
    conditionId: string;
    index: number;
};

type BranchDataType = {
    branchId: string;
    caseKey: string;
    index: number;
};

type LoopDataType = {
    index: number;
    loopId: string;
};

type LoopBreakDataType = {
    loopBreakId: string;
};

type ParallelDataType = {
    index: number;
    parallelId: string;
};

type EachDataType = {
    eachId: string;
    index: number;
};

export type TaskDispatcherDataType = BranchDataType &
    EachDataType &
    LoopDataType &
    LoopBreakDataType &
    ConditionDataType &
    ParallelDataType;

export type ClusterElementItemType = {
    label?: string;
    name: string;
    type: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
};

type ClusterElementValueType = ClusterElementItemType | ClusterElementItemType[] | null;

export type ClusterElementsType = {
    [key: string]: ClusterElementValueType;
};

export type NodeDataType = {
    branchData?: BranchDataType;
    branchId?: string;
    clusterElements?: ClusterElementsType;
    clusterElementName?: string;
    clusterElementType?: string;
    componentName: string;
    conditionCase?: 'caseTrue' | 'caseFalse';
    conditionData?: ConditionDataType;
    connection?: ConnectionDefinitionBasic;
    connections?: Array<ComponentConnection>;
    conditionId?: string;
    connectionId?: number;
    description?: string;
    displayConditions?: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: any;
    };
    eachId?: string;
    eachData?: EachDataType;
    icon?: JSX.Element | ReactNode | string;
    label?: string;
    loopBreakData?: LoopBreakDataType;
    loopData?: LoopDataType;
    loopId?: string;
    metadata?: {
        ui?: {
            dynamicPropertyTypes?: {[key: string]: string};
        };
    };
    multipleClusterElementsNode?: boolean;
    name: string;
    operationName?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    parallelData?: {
        parallelId: string;
        index: number;
    };
    rootClusterElement?: boolean;
    taskDispatcher?: boolean;
    taskDispatcherId?: string;
    title?: string;
    trigger?: boolean;
    type?: string;
    triggerType?: TriggerType;
    version?: number;
    workflowNodeName: string;
};

export type BranchCaseType = {
    key: string;
    tasks: Array<WorkflowTask>;
};

export type SubPropertyType = PropertyAllType & {custom: boolean};

export type WorkflowDefinitionType = {
    description?: string;
    label?: string;
    inputs?: Array<WorkflowInput>;
    outputs?: Array<WorkflowOutputType>;
    tasks?: Array<WorkflowTaskType>;
    triggers?: Array<WorkflowTriggerType>;
};

export type WorkflowOutputType = {
    name: string;
    value: object;
};

export type WorkflowTaskType = {
    clusterElements?: ClusterElementsType;
    connections: [
        {
            componentName: string;
            componentVersion: number;
            key: string;
            required: boolean;
            workflowNodeName: string;
        },
    ] & {
        [key: string]: ComponentConnectionType;
    };
    finalize?: Array<WorkflowTaskType>;
    label?: string;
    name: string;
    node?: string;
    parameters?: {[key: string]: object | []};
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

export type ComponentConnectionType = {
    componentName: string;
    componentVersion: number;
};

export type ArrayPropertyType = Property & {
    additionalProperties?: Array<Property>;
    controlType?: ControlType;
    custom?: boolean;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    defaultValue?: any;
    items?: Array<Property>;
    key?: string;
    label?: string;
    placeholder?: string;
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
    // NullProperty &
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

export type UpdateWorkflowMutationType = UseMutationResult<void, Error, UpdateWorkflowRequestI, unknown>;

export type TaskDispatcherContextType = {
    branchId?: string;
    caseKey?: string;
    conditionCase?: 'caseTrue' | 'caseFalse';
    conditionId?: string;
    eachId?: string;
    index?: number;
    loopBreakId?: string;
    loopId?: string;
    parallelId?: string;
    taskDispatcherId: string;
};

export type BuildNodeDataType = {
    taskDispatcherContext: TaskDispatcherContextType;
    taskDispatcherId: string;
    baseNodeData: NodeDataType;
};

export type UpdateTaskParametersType = {
    context?: TaskDispatcherContextType;
    task: WorkflowTask;
    updatedSubtasks: Array<WorkflowTask>;
};

export type BranchChildTasksType = {[branchId: string]: {cases: {[caseKey: string]: string[]}; default: string[]}};
export type ConditionChildTasksType = {[conditionId: string]: {caseTrue: string[]; caseFalse: string[]}};
export type EachChildTasksType = {[eachId: string]: {iteratee: string}};
export type LoopChildTasksType = {[loopId: string]: {iteratee: string[]}};
export type ParallelChildTasksType = {[parallelId: string]: {tasks: string[]}};

export type WorkflowInputType = WorkflowInput & {
    testValue?: string;
};

export type StructureParentType = 'INTEGRATION' | 'PROJECT';
