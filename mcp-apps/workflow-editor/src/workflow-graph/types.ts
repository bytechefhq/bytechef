export type WorkflowTaskType = {
    clusterRoot?: boolean;
    label?: string;
    name: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    type: string;
    [key: string]: unknown;
};

export type WorkflowTriggerType = {
    label?: string;
    name: string;
    parameters?: Record<string, unknown>;
    type: string;
    [key: string]: unknown;
};

export type ParsedWorkflowType = {
    label?: string;
    tasks?: WorkflowTaskType[];
    triggers?: WorkflowTriggerType[];
};

export type ReadOnlyNodeDataType = {
    componentName: string;
    label: string;
    name: string;
    operationName?: string;
    title?: string;
    trigger?: boolean;
};

export type ConditionDataType = {
    conditionCase: 'caseTrue' | 'caseFalse';
    conditionId: string;
    index: number;
};

export type OnErrorDataType = {
    index: number;
    onErrorCase: 'mainBranch' | 'onErrorBranch';
    onErrorId: string;
};

export type BranchDataType = {
    branchId: string;
    caseKey: string | number;
    index: number;
};

export type LoopDataType = {
    index: number;
    loopId: string;
};

export type MapDataType = {
    index: number;
    mapId: string;
};

export type LoopBreakDataType = {
    loopBreakId: string;
};

export type SubflowDataType = {
    subflowId: string;
};

export type TerminateDataType = {
    terminateId: string;
};

export type ParallelDataType = {
    index: number;
    parallelId: string;
};

export type EachDataType = {
    eachId: string;
    index: number;
};

export type ForkJoinDataType = {
    branchIndex: number;
    forkJoinId: string;
    index: number;
};

export type BranchCaseType = {
    key: string | number;
    tasks: Array<WorkflowTaskType>;
};

export type NodeDataType = {
    branchData?: BranchDataType;
    branchId?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    clusterElements?: any;
    clusterRoot?: boolean;
    componentName: string;
    conditionCase?: 'caseTrue' | 'caseFalse';
    conditionData?: ConditionDataType;
    conditionId?: string;
    description?: string;
    eachId?: string;
    eachData?: EachDataType;
    forkJoinId?: string;
    forkJoinData?: ForkJoinDataType;
    isNestedBottomGhost?: boolean;
    isNestedClusterRoot?: boolean;
    label?: string;
    loopBreakData?: LoopBreakDataType;
    loopData?: LoopDataType;
    loopId?: string;
    mapData?: MapDataType;
    mapId?: string;
    maxRetries?: number;
    onErrorData?: OnErrorDataType;
    onErrorId?: string;
    metadata?: {
        ui?: {
            chainAlignedPosition?: {x: number; y: number};
            dynamicPropertyTypes?: {[key: string]: string};
            nodePosition?: {x: number; y: number};
            placeholderPositions?: Record<string, {x: number; y: number}>;
        };
    };
    name: string;
    operationName?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    parallelData?: ParallelDataType;
    subflowData?: SubflowDataType;
    taskDispatcher?: boolean;
    taskDispatcherId?: string;
    terminateData?: TerminateDataType;
    terminateId?: string;
    title?: string;
    trigger?: boolean;
    type?: string;
    workflowNodeName?: string;
};

export type WorkflowNodeDataType = NodeDataType & ReadOnlyNodeDataType;

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
    mapId?: string;
    onErrorCase?: 'mainBranch' | 'onErrorBranch';
    onErrorId?: string;
    parallelId?: string;
    subflowId?: string;
    taskDispatcherId: string;
    terminateId?: string;
};

export type BuildNodeDataType = {
    taskDispatcherContext: TaskDispatcherContextType;
    taskDispatcherId: string;
    baseNodeData: NodeDataType;
};

export type UpdateTaskParametersType = {
    context?: TaskDispatcherContextType;
    task: WorkflowTaskType;
    updatedSubtasks: Array<WorkflowTaskType>;
};

export type PropertyAllType = {
    [key: string]: unknown;
};

export type BranchChildTasksType = {
    [branchId: string]: {cases: {[caseKey: string | number]: string[]}; default: string[]};
};
export type ConditionChildTasksType = {[conditionId: string]: {caseTrue: string[]; caseFalse: string[]}};
export type OnErrorChildTasksType = {
    [onErrorId: string]: {mainBranch: string[]; onErrorBranch: string[]};
};
export type EachChildTasksType = {[eachId: string]: {iteratee: string}};
export type LoopChildTasksType = {[loopId: string]: {iteratee: string[]}};
export type MapChildTasksType = {[mapId: string]: {iteratee: string[]}};
export type ParallelChildTasksType = {[parallelId: string]: {tasks: string[]}};
export type ForkJoinChildTasksType = {[forkJoinId: string]: {branches: string[][]}};
