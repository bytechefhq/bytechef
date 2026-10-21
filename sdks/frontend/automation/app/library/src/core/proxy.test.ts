import {runInNewContext} from 'node:vm';

import {describe, expect, it, vi} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createProxyApi, encodeProxyBody} from './proxy';
import {createRequester} from './request';

function proxyWith(...responses: Parameters<typeof mockFetch>) {
    const {calls, fetch} = mockFetch(...responses);
    const request = createRequester(() => ({appKey: 'k', baseUrl: 'https://bc.example', fetch, timeoutMs: 1000}));

    return {calls, proxy: createProxyApi(request)};
}

describe('encodeProxyBody', () => {
    it('sends a string as UTF8', () => {
        expect(encodeProxyBody('hi', undefined)).toEqual({body: 'hi', bodyEncoding: 'UTF8'});
    });

    it('sends bytes as BASE64', () => {
        expect(encodeProxyBody(new Uint8Array([104, 105]), 'application/octet-stream')).toEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            contentType: 'application/octet-stream',
        });
    });

    it('sends an object as JSON with a JSON content type unless one is given', () => {
        expect(encodeProxyBody({a: 1}, undefined)).toEqual({
            body: '{"a":1}',
            bodyEncoding: 'UTF8',
            contentType: 'application/json',
        });
        expect(encodeProxyBody([1], 'application/vnd.api+json')).toEqual({
            body: '[1]',
            bodyEncoding: 'UTF8',
            contentType: 'application/vnd.api+json',
        });
    });

    it('sends nothing for an absent body', () => {
        expect(encodeProxyBody(undefined, undefined)).toEqual({});
    });

    it('defaults bytes to an octet-stream content type', () => {
        expect(encodeProxyBody(new Uint8Array([1]), undefined)).toEqual({
            body: 'AQ==',
            bodyEncoding: 'BASE64',
            contentType: 'application/octet-stream',
        });
    });

    it('keeps an explicit content type for bytes', () => {
        expect(encodeProxyBody(new Uint8Array([1]), 'image/png')).toEqual({
            body: 'AQ==',
            bodyEncoding: 'BASE64',
            contentType: 'image/png',
        });
    });

    it('keeps an explicit content type for a string', () => {
        expect(encodeProxyBody('a,b', 'text/csv')).toEqual({
            body: 'a,b',
            bodyEncoding: 'UTF8',
            contentType: 'text/csv',
        });
    });

    it('keeps only the content type when there is no body', () => {
        expect(encodeProxyBody(undefined, 'text/plain')).toEqual({contentType: 'text/plain'});
    });
});

describe('proxy', () => {
    it('returns an upstream 404 as a result, not an error', async () => {
        const {calls, proxy} = proxyWith(
            jsonResponse({
                body: {
                    body: '{"error":"nope"}',
                    bodyEncoding: 'UTF8',
                    headers: {'content-type': ['application/json']},
                    status: 404,
                },
            })
        );

        const result = await proxy('crm', {method: 'GET', path: '/v3/objects/1', query: {limit: ['1']}});

        expect(result.status).toBe(404);
        expect(result.body).toBe('{"error":"nope"}');
        expect(result.bodyEncoding).toBe('UTF8');
        expect(result.headers['content-type']).toEqual(['application/json']);
        expect(result.text()).toBe('{"error":"nope"}');
        expect(result.json()).toEqual({error: 'nope'});
        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({
            method: 'GET',
            path: '/v3/objects/1',
            query: {limit: ['1']},
        });
    });

    it('throws a ByteChef refusal', async () => {
        const {proxy} = proxyWith(
            jsonResponse({body: {code: 'APP_CONNECTION_NOT_FOUND', message: 'no binding'}, status: 404})
        );

        await expect(proxy('nope', {method: 'GET', path: '/x'})).rejects.toMatchObject({
            code: 'APP_CONNECTION_NOT_FOUND',
            status: 404,
        });
    });

    it('decodes a BASE64 body into bytes and text', async () => {
        const {proxy} = proxyWith(
            jsonResponse({body: {body: 'aGk=', bodyEncoding: 'BASE64', headers: {}, status: 200}})
        );

        const result = await proxy('crm', {method: 'GET', path: '/file'});

        expect(Array.from(result.bytes())).toEqual([104, 105]);
        expect(result.body).toBe('aGk=');
        expect(result.bodyEncoding).toBe('BASE64');
        expect(result.text()).toBe('hi');
    });

    it('handles a null body', async () => {
        const {proxy} = proxyWith(jsonResponse({body: {body: null, bodyEncoding: 'UTF8', headers: {}, status: 204}}));

        const result = await proxy('crm', {method: 'DELETE', path: '/x'});

        expect(result.text()).toBe('');
        expect(result.bytes()).toHaveLength(0);
        expect(() => result.json()).toThrowError();
    });
});

describe('proxy request encoding', () => {
    it('sends the encoded body together with the headers, method, path and query', async () => {
        const {calls, proxy} = proxyWith(
            jsonResponse({body: {body: null, bodyEncoding: 'UTF8', headers: {}, status: 200}})
        );

        await proxy('crm', {
            body: {a: 1},
            headers: {'x-trace': ['t1']},
            method: 'POST',
            path: '/v3/objects',
            query: {limit: ['1']},
        });

        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({
            body: '{"a":1}',
            bodyEncoding: 'UTF8',
            contentType: 'application/json',
            headers: {'x-trace': ['t1']},
            method: 'POST',
            path: '/v3/objects',
            query: {limit: ['1']},
        });
    });

    it('URL-encodes the binding key', async () => {
        const {calls, proxy} = proxyWith(
            jsonResponse({body: {body: null, bodyEncoding: 'UTF8', headers: {}, status: 200}})
        );

        await proxy('a b', {method: 'GET', path: '/x'});

        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/connections/a%20b/proxy');
    });

    it('rejects with ABORTED and sends nothing for an already-aborted signal', async () => {
        const {calls, proxy} = proxyWith(
            jsonResponse({body: {body: null, bodyEncoding: 'UTF8', headers: {}, status: 200}})
        );
        const controller = new AbortController();

        controller.abort();

        await expect(proxy('crm', {method: 'GET', path: '/x'}, {signal: controller.signal})).rejects.toMatchObject({
            code: 'ABORTED',
        });
        expect(calls).toHaveLength(0);
    });
});

describe('proxy BASE64 bodies', () => {
    it('round-trips a body larger than one encoding chunk', async () => {
        const original = Uint8Array.from({length: 70_000}, (_value, index) => index % 251);
        const encoded = encodeProxyBody(original, undefined);
        const {proxy} = proxyWith(
            jsonResponse({body: {body: encoded.body, bodyEncoding: 'BASE64', headers: {}, status: 200}})
        );

        const result = await proxy('crm', {method: 'GET', path: '/big'});

        expect(result.bytes()).toHaveLength(original.length);
        expect(Array.from(result.bytes())).toEqual(Array.from(original));
    });

    it('decodes non-ASCII BASE64 text as UTF-8', async () => {
        const wire = btoa(String.fromCharCode(...new TextEncoder().encode('Grüße 👋')));
        const {proxy} = proxyWith(jsonResponse({body: {body: wire, bodyEncoding: 'BASE64', headers: {}, status: 200}}));

        const result = await proxy('crm', {method: 'GET', path: '/text'});

        expect(result.text()).toBe('Grüße 👋');
    });
});

describe('encodeProxyBody binary bodies', () => {
    it('sends an ArrayBuffer as BASE64 with an octet-stream content type', () => {
        expect(encodeProxyBody(new Uint8Array([104, 105]).buffer, undefined)).toEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            contentType: 'application/octet-stream',
        });
    });

    it('sends only the bytes a view covers, not its whole backing buffer', () => {
        const backing = new Uint8Array([0, 104, 105, 0]);

        expect(encodeProxyBody(backing.subarray(1, 3), undefined).body).toBe('aGk=');
        expect(encodeProxyBody(new DataView(backing.buffer, 1, 2), undefined).body).toBe('aGk=');
    });

    it('sends a pooled Node Buffer as exactly its own bytes', () => {
        const pooled = Buffer.from('hi');

        expect(pooled.byteOffset).toBeGreaterThan(0);
        expect(encodeProxyBody(pooled, 'text/plain')).toEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            contentType: 'text/plain',
        });
    });

    it('sends a typed array other than Uint8Array as its raw bytes', () => {
        expect(encodeProxyBody(new Float32Array([1]), undefined).body).toBe('AACAPw==');
    });

    it('sends a typed array and an ArrayBuffer from another realm as bytes', () => {
        const foreignBytes = runInNewContext('new Uint8Array([104, 105])') as Uint8Array;
        const foreignBuffer = runInNewContext('new Uint8Array([104, 105]).buffer') as ArrayBuffer;

        expect(encodeProxyBody(foreignBytes, undefined)).toEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            contentType: 'application/octet-stream',
        });
        expect(encodeProxyBody(foreignBuffer, undefined).body).toBe('aGk=');
    });
});

describe('proxy result access', () => {
    it('returns a fresh copy from every bytes() call', async () => {
        const {proxy} = proxyWith(jsonResponse({body: {body: 'hi', bodyEncoding: 'UTF8', headers: {}, status: 200}}));

        const result = await proxy('crm', {method: 'GET', path: '/x'});
        const first = result.bytes();

        first[0] = 0;

        expect(Array.from(result.bytes())).toEqual([104, 105]);
        expect(result.bytes()).not.toBe(result.bytes());
    });

    it('decodes the text of a BASE64 body only when asked, and only once', async () => {
        const {proxy} = proxyWith(
            jsonResponse({body: {body: 'aGk=', bodyEncoding: 'BASE64', headers: {}, status: 200}})
        );

        const result = await proxy('crm', {method: 'GET', path: '/file'});
        const decodeSpy = vi.spyOn(TextDecoder.prototype, 'decode');

        try {
            expect(decodeSpy).not.toHaveBeenCalled();
            expect(result.text()).toBe('hi');
            expect(result.text()).toBe('hi');
            expect(decodeSpy).toHaveBeenCalledTimes(1);
        } finally {
            decodeSpy.mockRestore();
        }
    });
});

describe('proxy BASE64 validation', () => {
    it('rejects a BASE64 body that is not base64 with INVALID_RESPONSE and the ByteChef status', async () => {
        const {proxy} = proxyWith(
            jsonResponse({body: {body: '%%%', bodyEncoding: 'BASE64', headers: {}, status: 200}})
        );

        await expect(proxy('crm', {method: 'GET', path: '/file'})).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
            status: 200,
        });
    });
});
