import type {ToolSet} from 'ai';
import {describe, expect, it, vi} from 'vitest';

import {BytechefAppError, type BytechefClient} from '../index';
import {toAiSdkTools} from './tools';

const searchTool = {
    bindingKey: 'crm',
    componentName: 'hubspot',
    description: 'Search records',
    name: 'CRM__SEARCH_RECORDS',
    parameters: {properties: {q: {type: 'string'}}, type: 'object'},
    toolName: 'searchRecords',
};
const mailTool = {...searchTool, bindingKey: 'mail', description: undefined, name: 'MAIL__SEND', toolName: 'send'};
const refund = {
    available: true,
    description: 'Approve a refund',
    inputSchema: {properties: {orderId: {type: 'string'}}, type: 'object'},
    name: 'Approve refund',
    outputSchema: null,
    workflowKey: 'approve-refund',
};
const open = {...refund, description: null, inputSchema: null, name: 'Open form', workflowKey: 'open_form'};
const gone = {...refund, available: false, workflowKey: 'gone'};
const completed = {
    approvalFormUrl: null,
    error: null,
    result: {approved: true},
    runId: 'run-1',
    status: 'COMPLETED',
    workflowKey: 'approve-refund',
};

function fakeClient() {
    const executeByName = vi.fn().mockResolvedValue({id: 1});
    const executeWorkflow = vi.fn().mockResolvedValue(completed);
    const bytechef = {
        tools: {executeByName, list: vi.fn().mockResolvedValue([searchTool, mailTool])},
        workflows: {execute: executeWorkflow, list: vi.fn().mockResolvedValue([refund, open, gone])},
    } as unknown as BytechefClient;

    return {bytechef, executeByName, executeWorkflow};
}

function call(tools: ToolSet, name: string, input: Record<string, unknown>, abortSignal?: AbortSignal) {
    return tools[name]!.execute!(input, {abortSignal, context: {}, messages: [], toolCallId: 't-1'} as never);
}

function schemaOf(tools: ToolSet, name: string): unknown {
    return (tools[name]!.inputSchema as unknown as {jsonSchema: unknown}).jsonSchema;
}

describe('toAiSdkTools', () => {
    it('builds one tool per component tool, keyed by flat name', async () => {
        const {bytechef} = fakeClient();

        const tools = await toAiSdkTools(bytechef);

        expect(Object.keys(tools).sort()).toEqual(['CRM__SEARCH_RECORDS', 'MAIL__SEND']);
        expect(tools['CRM__SEARCH_RECORDS']!.description).toBe('Search records');
        expect(tools['MAIL__SEND']!.description).toBe('send on hubspot');
        expect(schemaOf(tools, 'CRM__SEARCH_RECORDS')).toEqual(searchTool.parameters);
    });

    it('executes a component tool through executeByName and forwards the abort signal', async () => {
        const {bytechef, executeByName} = fakeClient();
        const controller = new AbortController();
        const tools = await toAiSdkTools(bytechef);

        await expect(call(tools, 'CRM__SEARCH_RECORDS', {q: 'x'}, controller.signal)).resolves.toEqual({id: 1});
        expect(executeByName).toHaveBeenCalledWith('CRM__SEARCH_RECORDS', {q: 'x'}, {signal: controller.signal});
    });

    it.each([
        [['crm'], ['CRM__SEARCH_RECORDS']],
        [['MAIL__SEND'], ['MAIL__SEND']],
    ])('include %j keeps %j', async (include, expected) => {
        const {bytechef} = fakeClient();

        expect(Object.keys(await toAiSdkTools(bytechef, {include}))).toEqual(expected);
    });

    it('adds one tool per available workflow when workflows is true', async () => {
        const {bytechef} = fakeClient();

        const tools = await toAiSdkTools(bytechef, {workflows: true});

        expect(Object.keys(tools).sort()).toEqual(['APPROVE_REFUND', 'CRM__SEARCH_RECORDS', 'MAIL__SEND', 'OPEN_FORM']);
        expect(tools['APPROVE_REFUND']!.description).toBe('Approve a refund');
        expect(tools['OPEN_FORM']!.description).toBe('Open form');
        expect(schemaOf(tools, 'APPROVE_REFUND')).toEqual(refund.inputSchema);
        expect(schemaOf(tools, 'OPEN_FORM')).toEqual({additionalProperties: true, type: 'object'});
    });

    it('includes a workflow by its key', async () => {
        const {bytechef} = fakeClient();

        expect(Object.keys(await toAiSdkTools(bytechef, {include: ['approve-refund'], workflows: true}))).toEqual([
            'APPROVE_REFUND',
        ]);
    });

    it('runs a workflow with wait true and the abort signal and returns its result', async () => {
        const {bytechef, executeWorkflow} = fakeClient();
        const controller = new AbortController();
        const tools = await toAiSdkTools(bytechef, {workflows: true});

        await expect(call(tools, 'APPROVE_REFUND', {orderId: 'A-1'}, controller.signal)).resolves.toEqual({
            approved: true,
        });
        expect(executeWorkflow).toHaveBeenCalledWith(
            'approve-refund',
            {orderId: 'A-1'},
            {signal: controller.signal, wait: true}
        );
    });

    it("passes waitForWorkflows: 'approval' through as wait", async () => {
        const {bytechef, executeWorkflow} = fakeClient();
        const tools = await toAiSdkTools(bytechef, {waitForWorkflows: 'approval', workflows: true});

        await call(tools, 'APPROVE_REFUND', {});

        expect(executeWorkflow).toHaveBeenCalledWith('approve-refund', {}, {signal: undefined, wait: 'approval'});
    });

    it.each([
        [
            {approvalFormUrl: 'https://bc.example/resume/t', status: 'APPROVAL_REQUIRED'},
            'Approval required: a person must decide before the workflow continues. Decide at https://bc.example/resume/t',
        ],
        [
            {approvalFormUrl: null, status: 'APPROVAL_REQUIRED'},
            'Approval required: a person must decide before the workflow continues',
        ],
        [{error: {message: 'boom'}, status: 'FAILED'}, 'Workflow failed: boom'],
        [{status: 'CANCELLED'}, 'Workflow was cancelled'],
    ])('describes %j to the model', async (runFields, expected) => {
        const {bytechef, executeWorkflow} = fakeClient();
        const tools = await toAiSdkTools(bytechef, {workflows: true});

        executeWorkflow.mockResolvedValue({...completed, result: undefined, ...runFields});

        await expect(call(tools, 'APPROVE_REFUND', {})).resolves.toBe(expected);
    });

    it('returns a ByteChef refusal to the model as text', async () => {
        const {bytechef, executeByName} = fakeClient();
        const tools = await toAiSdkTools(bytechef);

        executeByName.mockRejectedValue(
            new BytechefAppError('APP_WORKFLOW_INPUT_INVALID', 'orderId is required', {status: 400})
        );

        await expect(call(tools, 'CRM__SEARCH_RECORDS', {})).resolves.toBe(
            'Error APP_WORKFLOW_INPUT_INVALID: orderId is required'
        );
    });

    it.each([
        [401, 'UNAUTHORIZED'],
        [403, 'APP_DISABLED'],
        [403, 'HTTP_ERROR'],
    ])('rethrows an HTTP %i so a rotated or disabled key reaches the developer', async (status, code) => {
        const {bytechef, executeByName, executeWorkflow} = fakeClient();
        const tools = await toAiSdkTools(bytechef, {workflows: true});

        executeByName.mockRejectedValue(new BytechefAppError(code, 'denied', {status}));
        executeWorkflow.mockRejectedValue(new BytechefAppError(code, 'denied', {status}));

        await expect(call(tools, 'CRM__SEARCH_RECORDS', {})).rejects.toMatchObject({code, status});
        await expect(call(tools, 'APPROVE_REFUND', {})).rejects.toMatchObject({code, status});
    });

    it('returns APP_OPERATION_NOT_ALLOWED to the model so it can pick an allowed operation', async () => {
        const {bytechef, executeByName} = fakeClient();
        const tools = await toAiSdkTools(bytechef);

        executeByName.mockRejectedValue(
            new BytechefAppError('APP_OPERATION_NOT_ALLOWED', 'allowed: searchRecords', {status: 403})
        );

        await expect(call(tools, 'CRM__SEARCH_RECORDS', {})).resolves.toBe(
            'Error APP_OPERATION_NOT_ALLOWED: allowed: searchRecords'
        );
    });

    it('rethrows ABORTED so a cancelled generation stops', async () => {
        const {bytechef, executeWorkflow} = fakeClient();
        const tools = await toAiSdkTools(bytechef, {workflows: true});

        executeWorkflow.mockRejectedValue(new BytechefAppError('ABORTED', 'stopped', {runId: 'run-1'}));

        await expect(call(tools, 'APPROVE_REFUND', {})).rejects.toMatchObject({code: 'ABORTED', runId: 'run-1'});
    });

    it('rethrows errors that are not ByteChef errors', async () => {
        const {bytechef, executeByName} = fakeClient();
        const tools = await toAiSdkTools(bytechef);

        executeByName.mockRejectedValue(new TypeError('bug'));

        await expect(call(tools, 'CRM__SEARCH_RECORDS', {})).rejects.toThrowError(TypeError);
    });

    it('falls back to an any-object schema when a component tool has no object schema', async () => {
        const {bytechef} = fakeClient();

        vi.mocked(bytechef.tools.list).mockResolvedValue([{...searchTool, parameters: {}}] as never);

        const tools = await toAiSdkTools(bytechef);

        expect(schemaOf(tools, 'CRM__SEARCH_RECORDS')).toEqual({additionalProperties: true, type: 'object'});
    });

    it('falls back to an any-object schema when a workflow input schema is not an object schema', async () => {
        const {bytechef} = fakeClient();

        vi.mocked(bytechef.workflows.list).mockResolvedValue([{...refund, inputSchema: {type: 'string'}}] as never);

        const tools = await toAiSdkTools(bytechef, {workflows: true});

        expect(schemaOf(tools, 'APPROVE_REFUND')).toEqual({additionalProperties: true, type: 'object'});
    });

    it('hides the message of a client-side failure from the model', async () => {
        const {bytechef, executeByName} = fakeClient();
        const tools = await toAiSdkTools(bytechef);

        const error = new BytechefAppError('NETWORK_ERROR', 'ECONNREFUSED 10.0.0.5:8080');
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

        executeByName.mockRejectedValue(error);

        try {
            await expect(call(tools, 'CRM__SEARCH_RECORDS', {})).resolves.toBe(
                'Error NETWORK_ERROR: ByteChef is unavailable'
            );
            expect(consoleError).toHaveBeenCalledWith(error);
        } finally {
            consoleError.mockRestore();
        }
    });

    it.each(['HTTP_ERROR', 'INVALID_RESPONSE', 'TIMEOUT'])(
        'logs a %s from a component tool and tells the model ByteChef is unavailable',
        async (code) => {
            const {bytechef, executeByName} = fakeClient();
            const tools = await toAiSdkTools(bytechef);
            const error = new BytechefAppError(code, 'details', {status: 502});
            const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

            executeByName.mockRejectedValue(error);

            try {
                await expect(call(tools, 'CRM__SEARCH_RECORDS', {})).resolves.toBe(
                    `Error ${code}: ByteChef is unavailable`
                );
                expect(consoleError).toHaveBeenCalledWith(error);
            } finally {
                consoleError.mockRestore();
            }
        }
    );

    it.each([
        [
            {runId: 'run-7'},
            'Error TIMEOUT: ByteChef did not answer in time; the workflow run may already have started (run run-7), so do not retry it blindly',
        ],
        [
            {},
            'Error TIMEOUT: ByteChef did not answer in time; the workflow run may already have started, so do not retry it blindly',
        ],
    ])('warns the model not to retry a workflow tool TIMEOUT %j', async (errorOptions, expected) => {
        const {bytechef, executeWorkflow} = fakeClient();
        const tools = await toAiSdkTools(bytechef, {workflows: true});
        const error = new BytechefAppError('TIMEOUT', 'ByteChef did not answer within 30000 ms', errorOptions);
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

        executeWorkflow.mockRejectedValue(error);

        try {
            await expect(call(tools, 'APPROVE_REFUND', {})).resolves.toBe(expected);
            expect(consoleError).toHaveBeenCalledWith(error);
        } finally {
            consoleError.mockRestore();
        }
    });

    it('passes waitForWorkflows: false through as wait: false', async () => {
        const {bytechef, executeWorkflow} = fakeClient();
        const tools = await toAiSdkTools(bytechef, {waitForWorkflows: false, workflows: true});

        await call(tools, 'APPROVE_REFUND', {});

        expect(executeWorkflow).toHaveBeenCalledWith('approve-refund', {}, {signal: undefined, wait: false});
    });

    it.each([
        [{status: 'RUNNING'}, 'Workflow is still running (run run-1)'],
        [{error: null, status: 'FAILED'}, 'Workflow failed: no details'],
        [{result: undefined, status: 'COMPLETED'}, null],
    ])('describes %j to the model without details', async (runFields, expected) => {
        const {bytechef, executeWorkflow} = fakeClient();
        const tools = await toAiSdkTools(bytechef, {workflows: true});

        executeWorkflow.mockResolvedValue({...completed, ...runFields});

        await expect(call(tools, 'APPROVE_REFUND', {})).resolves.toBe(expected);
    });

    it('rethrows ABORTED from a component tool', async () => {
        const {bytechef, executeByName} = fakeClient();
        const tools = await toAiSdkTools(bytechef);

        executeByName.mockRejectedValue(new BytechefAppError('ABORTED', 'stopped'));

        await expect(call(tools, 'CRM__SEARCH_RECORDS', {})).rejects.toMatchObject({code: 'ABORTED'});
    });

    it('returns no tools for an empty include list', async () => {
        const {bytechef} = fakeClient();

        expect(Object.keys(await toAiSdkTools(bytechef, {include: [], workflows: true}))).toEqual([]);
    });
});
