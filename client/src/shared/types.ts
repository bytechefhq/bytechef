import {UpdateWorkflowRequestI} from '@/shared//mutations/platform/workflows.mutations';
import {
    ArrayProperty,
    BooleanProperty,
    ClusterElementDefinitionBasic,
    ClusterElementType,
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

export type WorkflowNodeType = {
    name: string;
    version: number;
    operationName: string;
    workflowNodeName?: string;
};

export type ComponentPropertiesType =
    | {
          componentDefinition: ComponentDefinitionBasic;
          properties?: Array<Property>;
      }
    | undefined;

export type ComponentType = {
    componentName: string;
    connections?: Array<ComponentConnection>;
    connectionId?: number;
    description?: string;
    displayConditions?: {
        [key: string]: boolean;
    };
    label?: string;
    metadata?: {
        ui?: {
            condition?: string;
            dynamicPropertyTypes?: {[key: string]: string};
            fromAi?: Array<string>;
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

export type DefinitionType = (ComponentDefinitionBasic | TaskDispatcherDefinition) & {
    taskDispatcher: boolean;
    trigger: boolean;
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

export type TabValueType = 'input' | 'output' | 'error' | 'logs';

type ConditionDataType = {
    conditionCase: 'caseTrue' | 'caseFalse';
    conditionId: string;
    index: number;
};

type BranchDataType = {
    branchId: string;
    caseKey: string | number;
    index: number;
};

type LoopDataType = {
    index: number;
    loopId: string;
};

type LoopBreakDataType = {
    loopBreakId: string;
};

type SubflowDataType = {
    subflowId: string;
};

type ParallelDataType = {
    index: number;
    parallelId: string;
};

type EachDataType = {
    eachId: string;
    index: number;
};

type ForkJoinDataType = {
    branchIndex: number;
    forkJoinId: string;
    index: number;
};

export type TaskDispatcherDataType = BranchDataType &
    EachDataType &
    LoopDataType &
    LoopBreakDataType &
    SubflowDataType &
    ConditionDataType &
    ParallelDataType &
    ForkJoinDataType;

export type NestedClusterRootComponentDefinitionType = {
    actionClusterElementTypes: {[key: string]: Array<string>};
    clusterElementClusterElementTypes: {[key: string]: Array<string>};
    clusterElementTypes: Array<ClusterElementType>;
};

export type ClusterElementItemType = {
    clusterElements?: ClusterElementsType;
    clusterElementTypesCount?: number;
    connections?: {[key: string]: ComponentConnectionType};
    label?: string;
    isNestedClusterRoot?: boolean;
    metadata?: {
        ui?: {
            nodePosition?: {x: number; y: number};
            placeholderPositions?: Record<string, {x: number; y: number}>;
        };
    };
    name: string;
    parentClusterRootId?: string;
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
    clusterElements?: ClusterElementsType | Array<ClusterElementDefinitionBasic>;
    clusterElementName?: string;
    clusterElementType?: string;
    clusterRoot?: boolean;
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
    forkJoinId?: string;
    forkJoinData?: ForkJoinDataType;
    icon?: JSX.Element | ReactNode | string;
    isNestedClusterRoot?: boolean;
    label?: string;
    loopBreakData?: LoopBreakDataType;
    loopData?: LoopDataType;
    loopId?: string;
    metadata?: {
        ui?: {
            dynamicPropertyTypes?: {[key: string]: string};
            nodePosition?: {x: number; y: number};
            placeholderPositions?: Record<string, {x: number; y: number}>;
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
    parentClusterRootId?: string;
    subflowData?: SubflowDataType;
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
    key: string | number;
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
    properties?: Array<PropertyAllType>;
};

export type UpdateWorkflowMutationType = UseMutationResult<void, Error, UpdateWorkflowRequestI, unknown>;

export type TaskDispatcherContextType = {
    branchIndex?: number;
    branchId?: string;
    caseKey?: string | number;
    conditionCase?: 'caseTrue' | 'caseFalse';
    conditionId?: string;
    eachId?: string;
    forkJoinId?: string;
    index?: number;
    loopBreakId?: string;
    loopId?: string;
    parallelId?: string;
    subflowId?: string;
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

export type BranchChildTasksType = {
    [branchId: string]: {cases: {[caseKey: string | number]: string[]}; default: string[]};
};
export type ConditionChildTasksType = {[conditionId: string]: {caseTrue: string[]; caseFalse: string[]}};
export type EachChildTasksType = {[eachId: string]: {iteratee: string}};
export type LoopChildTasksType = {[loopId: string]: {iteratee: string[]}};
export type ParallelChildTasksType = {[parallelId: string]: {tasks: string[]}};
export type ForkJoinChildTasksType = {[forkJoinId: string]: {branches: string[][]}};

export type WorkflowInputType = WorkflowInput & {
    testValue?: string;
};

export type SelectOptionType = {
    description?: string;
    label: string;
    value: string;
};
