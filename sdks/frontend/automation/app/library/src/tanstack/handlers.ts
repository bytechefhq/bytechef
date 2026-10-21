import type {BytechefContext} from './middleware';
import type {
    CallApiInput,
    ExecuteActionInput,
    ExecuteToolByNameInput,
    ExecuteToolInput,
    ExecuteWorkflowInput,
    GetWorkflowRunInput,
    ProxyInput,
} from './validators';

interface HandlerArguments<Data> {
    context: BytechefContext;
    data: Data;
}

export type JsonValue = string | number | boolean | null | JsonValue[] | {[key: string]: JsonValue};

/**
 * `T` with every `unknown` replaced by `JsonValue`. A value typed `unknown` comes from a JSON-parsed answer.
 */
export type JsonSerializable<T> = unknown extends T
    ? JsonValue
    : T extends readonly (infer Item)[]
      ? JsonSerializable<Item>[]
      : T extends object
        ? {[Key in keyof T]: JsonSerializable<T[Key]>}
        : T;

/**
 * Resolves to the value of `promise`, which comes from a JSON-parsed answer, typed as `JsonSerializable<T>`.
 */
async function toSerializable<T>(promise: Promise<T>): Promise<JsonSerializable<T>> {
    return (await promise) as JsonSerializable<T>;
}

/**
 * Resolves to the JSON-parsed value of an answer, or `undefined` when the answer had an empty body.
 */
async function toJsonResult(promise: Promise<unknown>): Promise<JsonValue | undefined> {
    return (await promise) as JsonValue | undefined;
}

export interface SerializableProxyResult {
    body: string | null;
    bodyEncoding: 'BASE64' | 'UTF8';
    headers: Record<string, string[]>;
    status: number;
}

export const callApiHandler = ({context, data}: HandlerArguments<CallApiInput>) =>
    toSerializable(context.bytechef.api.call(data));

export const listApiCollectionsHandler = ({context}: HandlerArguments<undefined>) =>
    toSerializable(context.bytechef.api.list());

export const listConnectionsHandler = ({context}: HandlerArguments<undefined>) => context.bytechef.connections.list();

export const executeActionHandler = ({context, data}: HandlerArguments<ExecuteActionInput>) =>
    toJsonResult(
        context.bytechef.actions.execute(
            data.bindingKey,
            data.actionName,
            data.input,
            data.componentVersion === undefined ? {} : {componentVersion: data.componentVersion}
        )
    );

export const listToolsHandler = ({context}: HandlerArguments<undefined>) =>
    toSerializable(context.bytechef.tools.list());

export const executeToolHandler = ({context, data}: HandlerArguments<ExecuteToolInput>) =>
    toJsonResult(context.bytechef.tools.execute(data.bindingKey, data.toolName, data.input));

export const executeToolByNameHandler = ({context, data}: HandlerArguments<ExecuteToolByNameInput>) =>
    toJsonResult(context.bytechef.tools.executeByName(data.name, data.input));

export const proxyHandler = async ({context, data}: HandlerArguments<ProxyInput>): Promise<SerializableProxyResult> => {
    const result = await context.bytechef.proxy(data.bindingKey, data.request);

    return {body: result.body, bodyEncoding: result.bodyEncoding, headers: result.headers, status: result.status};
};

export const listWorkflowsHandler = ({context}: HandlerArguments<undefined>) =>
    toSerializable(context.bytechef.workflows.list());

export const executeWorkflowHandler = ({context, data}: HandlerArguments<ExecuteWorkflowInput>) =>
    toSerializable(
        context.bytechef.workflows.execute(
            data.workflowKey,
            data.input,
            data.waitSeconds === undefined ? {} : {waitSeconds: data.waitSeconds}
        )
    );

export const getWorkflowRunHandler = ({context, data}: HandlerArguments<GetWorkflowRunInput>) =>
    toSerializable(context.bytechef.workflows.getRun(data.runId));
