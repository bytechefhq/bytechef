/**
 * Type definitions for ByteChef script code workflows: the object a JavaScript code workflow file evaluates to, and
 * the context ByteChef passes to each task's `perform`.
 */

export type LogLevel = 'trace' | 'debug' | 'info' | 'warn' | 'error' | 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

/**
 * A cluster element composed at the call site, for actions that read cluster elements, such as the AI Agent's
 * `chat`.
 */
export interface ClusterElement {
    /** One of the task's declared connection names. */
    connection?: string;

    /** For a tool, the name the model calls it by. Defaults to the element name in `type`. */
    name?: string;

    parameters?: Record<string, unknown>;

    /** `<componentName>/v<version>/<elementName>`, for example `openAi/v1/model`. */
    type: string;
}

/**
 * Cluster elements keyed by cluster element type (`model`, `tools`, `chatMemory`, `rag`, ...). A type that takes
 * several elements takes a list.
 */
export type ClusterElements = Record<string, ClusterElement | ClusterElement[]>;

/**
 * Calls a component action: `input` is the action's input parameters, `connectionName` names one of the task's
 * declared connections, and `clusterElements` wires an action that reads cluster elements.
 */
export type ComponentAction = (
    input?: Record<string, unknown>,
    connectionName?: string | null,
    clusterElements?: ClusterElements,
) => any;

/**
 * The context ByteChef passes to `perform`.
 */
export interface TaskContext {
    /** `context.component.<componentName>.<actionName>(input, connectionName, clusterElements)`. */
    component: Record<string, Record<string, ComponentAction>>;

    /** A declared connection's parameters, credentials included. */
    connection(connectionName: string): Record<string, unknown>;

    /** The workflow inputs plus the output of every task that already ran, keyed by name. */
    input(): Record<string, unknown>;

    /** One workflow input or task output. Fails on an unknown name. */
    input(name: string): unknown;

    /** Writes to the task's execution log. */
    log(level: LogLevel, message: string): void;

    /** The task's declared parameters, with any `${...}` already evaluated. */
    parameters(): Record<string, unknown>;
}

/**
 * A connection a task uses. `name` is what `perform` passes as `connectionName`. `componentVersion` defaults to the
 * component's latest version at save time.
 */
export interface ConnectionRequirement {
    componentName: string;
    componentVersion?: number;
    name: string;
}

/**
 * A task: one step of a workflow. ByteChef calls `perform` when the task runs, and its return value becomes the
 * task's output. Task names must be unique across the workflow, nesting included.
 */
export interface TaskDefinition {
    /** A list, or a map keyed by connection name. */
    connections?: ConnectionRequirement[] | Record<string, Omit<ConnectionRequirement, 'name'>>;

    description?: string;
    label?: string;
    name: string;

    /** Read back with `context.parameters()`. A value may be a `${...}` expression. */
    parameters?: Record<string, unknown>;

    perform: (context: TaskContext) => unknown;
}

/**
 * A group whose tasks all start at the same time. They can't read each other's output.
 */
export interface ParallelTaskDefinition {
    description?: string;
    label?: string;
    name: string;
    tasks: TaskDefinition[];
    type: 'parallel';
}

/**
 * A group whose branches run at the same time. The tasks within a branch run in order.
 */
export interface ForkJoinTaskDefinition {
    branches: TaskDefinition[][];
    description?: string;
    label?: string;
    name: string;
    type: 'forkJoin';
}

/**
 * An entry of a workflow's `tasks`. A group can't contain another group.
 */
export type WorkflowTask = TaskDefinition | ParallelTaskDefinition | ForkJoinTaskDefinition;

export type InputType = 'BOOLEAN' | 'DATE' | 'DATE_TIME' | 'FIELD_MAPPING' | 'INTEGER' | 'NUMBER' | 'STRING' | 'TIME';

/**
 * A workflow input. At run time it arrives in `context.input()` under its own name.
 */
export interface InputDefinition {
    /** With `componentVersion` and `groupName`, the component input this input takes its options from. */
    componentName?: string;
    componentVersion?: number;
    groupName?: string;
    label?: string;
    name: string;
    required?: boolean;

    /** Defaults to `STRING`. */
    type?: InputType;
}

/**
 * A workflow output: either a task, whose output becomes the value, or a literal or `${...}` expression `value`.
 */
export type OutputDefinition = {name: string; task: string} | {name: string; value: unknown};

/**
 * A component trigger that starts the workflow, for example `schedule/v1/interval`.
 */
export interface TriggerDefinition {
    name: string;
    parameters?: Record<string, unknown>;

    /** `<componentName>/v<version>/<triggerName>`. */
    type: string;
}

export interface WorkflowDefinition {
    description?: string;
    inputs?: InputDefinition[];
    label?: string;
    name: string;
    outputs?: OutputDefinition[];
    tasks?: WorkflowTask[];
    triggers?: TriggerDefinition[];
}

/**
 * An automation code project, or an embedded catalog code project. `name` is the project's identity: deploying again
 * under the same name updates the project. `version` defaults to `0.0.1`.
 */
export interface ProjectDefinition {
    description?: string;
    name: string;
    version?: string;
    workflows?: WorkflowDefinition[];
}

/**
 * An embedded code integration, identified by `componentName` and `componentVersion`.
 */
export interface IntegrationDefinition {
    componentName: string;
    componentVersion: number;
    description?: string;
    version?: string;
    workflows?: WorkflowDefinition[];
}

/**
 * The same types as globals, for code workflow files: ByteChef evaluates a plain script, so the file can't `import`
 * them. Reference them with `/// <reference types="@bytechef/workflow-api" />` instead.
 */
declare global {
    namespace ByteChef {
        type ClusterElement = import('./index.js').ClusterElement;
        type ClusterElements = import('./index.js').ClusterElements;
        type ComponentAction = import('./index.js').ComponentAction;
        type ConnectionRequirement = import('./index.js').ConnectionRequirement;
        type ForkJoinTaskDefinition = import('./index.js').ForkJoinTaskDefinition;
        type InputDefinition = import('./index.js').InputDefinition;
        type InputType = import('./index.js').InputType;
        type IntegrationDefinition = import('./index.js').IntegrationDefinition;
        type LogLevel = import('./index.js').LogLevel;
        type OutputDefinition = import('./index.js').OutputDefinition;
        type ParallelTaskDefinition = import('./index.js').ParallelTaskDefinition;
        type ProjectDefinition = import('./index.js').ProjectDefinition;
        type TaskContext = import('./index.js').TaskContext;
        type TaskDefinition = import('./index.js').TaskDefinition;
        type TriggerDefinition = import('./index.js').TriggerDefinition;
        type WorkflowDefinition = import('./index.js').WorkflowDefinition;
        type WorkflowTask = import('./index.js').WorkflowTask;
    }
}
