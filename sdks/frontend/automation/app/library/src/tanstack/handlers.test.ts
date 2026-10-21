import {describe, expect, expectTypeOf, it, vi} from 'vitest';

import type {BytechefClient} from '../index';
import {
    callApiHandler,
    executeActionHandler,
    executeToolByNameHandler,
    executeToolHandler,
    executeWorkflowHandler,
    getWorkflowRunHandler,
    listApiCollectionsHandler,
    listConnectionsHandler,
    listToolsHandler,
    listWorkflowsHandler,
    proxyHandler,
    type JsonValue,
} from './handlers';

describe('handlers', () => {
    it('callApiHandler delegates to api.call', async () => {
        const response = {body: {id: 1}, headers: {}, status: 201};
        const bytechef = {api: {call: vi.fn().mockResolvedValue(response)}} as unknown as BytechefClient;
        const data = {body: {a: 1}, method: 'POST' as const, path: 'v1/crm/contacts'};

        await expect(callApiHandler({context: {bytechef}, data})).resolves.toEqual(response);
        expect(bytechef.api.call).toHaveBeenCalledWith(data);
    });

    it('listApiCollectionsHandler delegates to api.list', async () => {
        const bytechef = {api: {list: vi.fn().mockResolvedValue([{name: 'CRM'}])}} as unknown as BytechefClient;

        await expect(listApiCollectionsHandler({context: {bytechef}, data: undefined})).resolves.toEqual([
            {name: 'CRM'},
        ]);
    });

    it('listConnectionsHandler delegates to connections.list', async () => {
        const bytechef = {connections: {list: vi.fn().mockResolvedValue([{key: 'crm'}])}} as unknown as BytechefClient;

        await expect(listConnectionsHandler({context: {bytechef}, data: undefined})).resolves.toEqual([{key: 'crm'}]);
        expect(bytechef.connections.list).toHaveBeenCalledWith();
    });

    it('executeActionHandler delegates to actions.execute', async () => {
        const bytechef = {actions: {execute: vi.fn().mockResolvedValue({id: 1})}} as unknown as BytechefClient;

        await expect(
            executeActionHandler({
                context: {bytechef},
                data: {actionName: 'create', bindingKey: 'crm', componentVersion: 2, input: {a: 1}},
            })
        ).resolves.toEqual({id: 1});
        expect(bytechef.actions.execute).toHaveBeenCalledWith('crm', 'create', {a: 1}, {componentVersion: 2});
        expect(vi.mocked(bytechef.actions.execute).mock.calls[0]![3]).toStrictEqual({componentVersion: 2});
    });

    it('executeActionHandler passes empty options when no componentVersion is given', async () => {
        const bytechef = {actions: {execute: vi.fn().mockResolvedValue({id: 1})}} as unknown as BytechefClient;

        await executeActionHandler({
            context: {bytechef},
            data: {actionName: 'create', bindingKey: 'crm', input: {a: 1}},
        });

        expect(vi.mocked(bytechef.actions.execute).mock.calls[0]![3]).toStrictEqual({});
    });

    it('listToolsHandler delegates to tools.list', async () => {
        const bytechef = {tools: {list: vi.fn().mockResolvedValue([{name: 'CRM_FIND'}])}} as unknown as BytechefClient;

        await expect(listToolsHandler({context: {bytechef}, data: undefined})).resolves.toEqual([{name: 'CRM_FIND'}]);
        expect(bytechef.tools.list).toHaveBeenCalledWith();
    });

    it('executeToolHandler delegates to tools.execute', async () => {
        const bytechef = {tools: {execute: vi.fn().mockResolvedValue({ok: true})}} as unknown as BytechefClient;

        await expect(
            executeToolHandler({context: {bytechef}, data: {bindingKey: 'crm', input: {a: 1}, toolName: 'find'}})
        ).resolves.toEqual({ok: true});
        expect(bytechef.tools.execute).toHaveBeenCalledWith('crm', 'find', {a: 1});
    });

    it('executeToolByNameHandler delegates to tools.executeByName', async () => {
        const bytechef = {tools: {executeByName: vi.fn().mockResolvedValue({ok: true})}} as unknown as BytechefClient;

        await expect(
            executeToolByNameHandler({context: {bytechef}, data: {input: {a: 1}, name: 'CRM_FIND'}})
        ).resolves.toEqual({ok: true});
        expect(bytechef.tools.executeByName).toHaveBeenCalledWith('CRM_FIND', {a: 1});
    });

    it('proxyHandler returns the wire body, encoding, headers and status', async () => {
        const bytechef = {
            proxy: vi.fn().mockResolvedValue({
                body: 'aGk=',
                bodyEncoding: 'BASE64',
                bytes: vi.fn(),
                headers: {a: ['1']},
                json: vi.fn(),
                status: 200,
                text: vi.fn(),
            }),
        } as unknown as BytechefClient;

        await expect(
            proxyHandler({context: {bytechef}, data: {bindingKey: 'crm', request: {method: 'GET', path: '/f'}}})
        ).resolves.toStrictEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            headers: {a: ['1']},
            status: 200,
        });
        expect(bytechef.proxy).toHaveBeenCalledWith('crm', {method: 'GET', path: '/f'});
    });

    it('listWorkflowsHandler delegates to workflows.list', async () => {
        const bytechef = {workflows: {list: vi.fn().mockResolvedValue([{key: 'wf'}])}} as unknown as BytechefClient;

        await expect(listWorkflowsHandler({context: {bytechef}, data: undefined})).resolves.toEqual([{key: 'wf'}]);
        expect(bytechef.workflows.list).toHaveBeenCalledWith();
    });

    it('executeWorkflowHandler forwards only waitSeconds', async () => {
        const bytechef = {workflows: {execute: vi.fn().mockResolvedValue({runId: 'r1'})}} as unknown as BytechefClient;

        await expect(
            executeWorkflowHandler({
                context: {bytechef},
                data: {input: {a: 1}, waitSeconds: 5, workflowKey: 'wf'},
            })
        ).resolves.toEqual({runId: 'r1'});
        expect(vi.mocked(bytechef.workflows.execute).mock.calls[0]![0]).toBe('wf');
        expect(vi.mocked(bytechef.workflows.execute).mock.calls[0]![1]).toStrictEqual({a: 1});
        expect(vi.mocked(bytechef.workflows.execute).mock.calls[0]![2]).toStrictEqual({waitSeconds: 5});
    });

    it('executeWorkflowHandler passes empty options when waitSeconds is not given', async () => {
        const bytechef = {workflows: {execute: vi.fn().mockResolvedValue({runId: 'r1'})}} as unknown as BytechefClient;

        await executeWorkflowHandler({context: {bytechef}, data: {input: {}, workflowKey: 'wf'}});

        expect(vi.mocked(bytechef.workflows.execute).mock.calls[0]![2]).toStrictEqual({});
    });

    it('getWorkflowRunHandler delegates to workflows.getRun', async () => {
        const bytechef = {
            workflows: {getRun: vi.fn().mockResolvedValue({status: 'COMPLETED'})},
        } as unknown as BytechefClient;

        await expect(getWorkflowRunHandler({context: {bytechef}, data: {runId: 'r1'}})).resolves.toEqual({
            status: 'COMPLETED',
        });
        expect(bytechef.workflows.getRun).toHaveBeenCalledWith('r1');
    });

    it('types the results that can have an empty body as JsonValue or undefined', () => {
        expectTypeOf(executeActionHandler).returns.resolves.toEqualTypeOf<JsonValue | undefined>();
        expectTypeOf(executeToolHandler).returns.resolves.toEqualTypeOf<JsonValue | undefined>();
        expectTypeOf(executeToolByNameHandler).returns.resolves.toEqualTypeOf<JsonValue | undefined>();
    });
});
