import {describe, expect, it} from 'vitest';

import {jsonResponse, mockFetch} from '../test/mockFetch';
import {type BytechefClient, createBytechefClient} from './client';

type EndpointCall = [string, (client: BytechefClient) => Promise<unknown>, unknown];

const ENDPOINT_CALLS: EndpointCall[] = [
    ['connections.list', (client) => client.connections.list(), {}],
    ['actions.list', (client) => client.actions.list(), {}],
    ['actions.execute', (client) => client.actions.execute('crm', 'createRecord', {}), []],
    ['tools.list', (client) => client.tools.list(), {}],
    ['tools.execute', (client) => client.tools.execute('crm', 'searchRecords', {}), []],
    ['tools.executeByName', (client) => client.tools.executeByName('CRM__SEARCH_RECORDS', {}), {}],
    ['proxy', (client) => client.proxy('crm', {method: 'GET', path: '/x'}), []],
    ['workflows.list', (client) => client.workflows.list(), {}],
    ['workflows.execute', (client) => client.workflows.execute('approve', {}), []],
    ['workflows.getRun', (client) => client.workflows.getRun('run-1'), []],
];

function clientAnswering(response: Response): BytechefClient {
    const {fetch} = mockFetch(response);

    return createBytechefClient({appKey: 'k', fetch, url: 'https://bc.example'});
}

describe('an endpoint answering success without the body it promises', () => {
    it.each(ENDPOINT_CALLS)('%s rejects an empty 200 with INVALID_RESPONSE', async (_name, call) => {
        await expect(call(clientAnswering(new Response(null, {status: 200})))).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
            status: 200,
        });
    });

    it.each(ENDPOINT_CALLS)('%s rejects a JSON null with INVALID_RESPONSE', async (_name, call) => {
        await expect(call(clientAnswering(jsonResponse({text: 'null'})))).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
            status: 200,
        });
    });

    it.each(ENDPOINT_CALLS)(
        '%s rejects a JSON body of the wrong shape with INVALID_RESPONSE',
        async (_name, call, wrongShape) => {
            await expect(call(clientAnswering(jsonResponse({body: wrongShape})))).rejects.toMatchObject({
                code: 'INVALID_RESPONSE',
                status: 200,
            });
        }
    );

    it('names the path and the expected shape in the message', async () => {
        const error = await clientAnswering(jsonResponse({body: {}}))
            .connections.list()
            .catch((caught: unknown) => caught);

        expect((error as Error).message).toBe(
            'ByteChef answered 200 to /connections without a JSON array; check that BYTECHEF_URL points at the ByteChef server'
        );
    });
});
