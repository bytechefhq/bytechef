import {dynamicTool, jsonSchema, type JSONSchema7, type ToolSet} from 'ai';

import {BytechefAppError, type BytechefClient, type JsonSchema, type WaitMode, type WorkflowRun} from '../index';
import {toFlatKey} from './names';

export interface ToAiSdkToolsOptions {
    /**
     * Restricts the tools returned. Entries are matched against component tool flat names, component binding keys
     * and workflow keys (or their flat names); entries that match nothing are ignored. Omitted means every tool.
     */
    include?: string[];
    /**
     * How a workflow tool waits for its run. `true` (default) polls until the run finishes or needs approval,
     * `'approval'` keeps waiting through approvals, `false` returns at once with the still-running run described.
     */
    waitForWorkflows?: WaitMode;
    /**
     * Adds one tool per available app workflow, named by the workflow key flattened with `toFlatKey`: every run of
     * non-alphanumeric characters becomes `_`, then the result is upper-cased. Defaults to false.
     */
    workflows?: boolean;
}

const ANY_OBJECT: JsonSchema = {additionalProperties: true, type: 'object'};

const UNAVAILABLE_CODES = new Set(['HTTP_ERROR', 'INVALID_RESPONSE', 'NETWORK_ERROR', 'TIMEOUT']);

const APPROVAL_REQUIRED = 'Approval required: a person must decide before the workflow continues';

export function describeRun(run: WorkflowRun): unknown {
    switch (run.status) {
        case 'COMPLETED':
            return run.result ?? null;
        case 'APPROVAL_REQUIRED':
            return run.approvalFormUrl ? `${APPROVAL_REQUIRED}. Decide at ${run.approvalFormUrl}` : APPROVAL_REQUIRED;
        case 'FAILED':
            return `Workflow failed: ${run.error?.message ?? 'no details'}`;
        case 'CANCELLED':
            return 'Workflow was cancelled';
        default:
            return `Workflow is still running (run ${run.runId})`;
    }
}

function isAuthenticationFailure(error: BytechefAppError): boolean {
    if (error.status === 401) {
        return true;
    }

    return error.status === 403 && (error.code === 'APP_DISABLED' || error.code === 'HTTP_ERROR');
}

function workflowTimeoutMessage(error: BytechefAppError): string {
    const run = error.runId ? ` (run ${error.runId})` : '';

    return `Error TIMEOUT: ByteChef did not answer in time; the workflow run may already have started${run}, so do not retry it blindly`;
}

/**
 * Runs a tool call and turns a `BytechefAppError` into text for the model. `ABORTED`, HTTP 401 and a 403 for a
 * disabled app or without a server code (a rotated or disabled app key) are rethrown so they reach the developer;
 * `APP_OPERATION_NOT_ALLOWED` reaches the model, whose message lists the allowed operations. Client-side failures are
 * logged with `console.error` before the model sees a redacted text.
 */
async function guarded<T>(call: () => Promise<T>, workflow = false): Promise<T | string> {
    try {
        return await call();
    } catch (error) {
        if (!(error instanceof BytechefAppError) || error.code === 'ABORTED' || isAuthenticationFailure(error)) {
            throw error;
        }

        if (!UNAVAILABLE_CODES.has(error.code)) {
            return `Error ${error.code}: ${error.message}`;
        }

        console.error(error);

        return workflow && error.code === 'TIMEOUT'
            ? workflowTimeoutMessage(error)
            : `Error ${error.code}: ByteChef is unavailable`;
    }
}

function objectSchema(schema: unknown): JSONSchema7 {
    const isObjectSchema =
        typeof schema === 'object' && schema !== null && (schema as {type?: unknown}).type === 'object';

    return (isObjectSchema ? schema : ANY_OBJECT) as JSONSchema7;
}

function included(include: string[] | undefined, ...candidates: string[]): boolean {
    return include === undefined || candidates.some((candidate) => include.includes(candidate));
}

export async function toAiSdkTools(bytechef: BytechefClient, options: ToAiSdkToolsOptions = {}): Promise<ToolSet> {
    const tools: ToolSet = {};

    for (const tool of await bytechef.tools.list()) {
        if (!included(options.include, tool.name, tool.bindingKey)) {
            continue;
        }

        tools[tool.name] = dynamicTool({
            description: tool.description ?? `${tool.toolName} on ${tool.componentName}`,
            execute: (input, {abortSignal}) =>
                guarded(() =>
                    bytechef.tools.executeByName(tool.name, input as Record<string, unknown>, {signal: abortSignal})
                ),
            inputSchema: jsonSchema(objectSchema(tool.parameters)),
        });
    }

    if (options.workflows) {
        for (const workflow of await bytechef.workflows.list()) {
            const name = toFlatKey(workflow.workflowKey);

            if (!workflow.available || !included(options.include, name, workflow.workflowKey)) {
                continue;
            }

            tools[name] = dynamicTool({
                description: workflow.description ?? workflow.name,
                execute: (input, {abortSignal}) =>
                    guarded(
                        async () =>
                            describeRun(
                                await bytechef.workflows.execute(
                                    workflow.workflowKey,
                                    input as Record<string, unknown>,
                                    {
                                        signal: abortSignal,
                                        wait: options.waitForWorkflows ?? true,
                                    }
                                )
                            ),
                        true
                    ),
                inputSchema: jsonSchema(objectSchema(workflow.inputSchema)),
            });
        }
    }

    return tools;
}
