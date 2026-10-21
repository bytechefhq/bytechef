import {describe, expect, it} from 'vitest';

import {
    callApiInput,
    executeActionInput,
    executeToolByNameInput,
    executeToolInput,
    executeWorkflowInput,
    getWorkflowRunInput,
    proxyInput,
} from './validators';

const validators: Array<[string, (input: unknown) => unknown, Record<string, unknown>, string]> = [
    ['callApiInput', callApiInput, {method: 'POST', path: 'v1/crm/contacts', query: {a: ['1']}}, 'path'],
    [
        'executeActionInput',
        executeActionInput,
        {actionName: 'create', bindingKey: 'crm', componentVersion: 2, input: {a: 1}},
        'bindingKey',
    ],
    ['executeToolInput', executeToolInput, {bindingKey: 'crm', input: {a: 1}, toolName: 'find'}, 'bindingKey'],
    ['executeToolByNameInput', executeToolByNameInput, {input: {a: 1}, name: 'CRM_FIND'}, 'name'],
    ['proxyInput', proxyInput, {bindingKey: 'crm', request: {method: 'GET', path: '/f'}}, 'bindingKey'],
    ['executeWorkflowInput', executeWorkflowInput, {input: {a: 1}, workflowKey: 'wf'}, 'workflowKey'],
    ['getWorkflowRunInput', getWorkflowRunInput, {runId: 'r1'}, 'runId'],
];

describe('validators', () => {
    it.each(validators)('%s returns a valid input as the typed object', (_name, validator, valid) => {
        expect(validator(valid)).toEqual(valid);
    });

    it.each(validators)('%s throws a TypeError naming a missing required string', (_name, validator, valid, field) => {
        const missing = {...valid};

        delete missing[field];

        expect(() => validator(missing)).toThrow(TypeError);
        expect(() => validator(missing)).toThrow(new RegExp(field));
    });

    it.each(validators)('%s throws on an empty required string', (_name, validator, valid, field) => {
        expect(() => validator({...valid, [field]: ''})).toThrow(new RegExp(field));
    });

    it.each(validators)('%s rejects a non-object input', (_name, validator) => {
        expect(() => validator('text')).toThrow(TypeError);
        expect(() => validator(null)).toThrow(TypeError);
        expect(() => validator([])).toThrow(TypeError);
        expect(() => validator(undefined)).toThrow(TypeError);
    });

    it.each([
        ['executeActionInput', executeActionInput, {actionName: 'a', bindingKey: 'b'}],
        ['executeToolInput', executeToolInput, {bindingKey: 'b', toolName: 't'}],
        ['executeToolByNameInput', executeToolByNameInput, {name: 'n'}],
        ['executeWorkflowInput', executeWorkflowInput, {workflowKey: 'w'}],
    ] as Array<[string, (input: unknown) => unknown, Record<string, unknown>]>)(
        '%s rejects a missing or non-object input.input',
        (_name, validator, base) => {
            expect(() => validator(base)).toThrow(/input\.input/);
            expect(() => validator({...base, input: 'x'})).toThrow(/input\.input/);
            expect(() => validator({...base, input: [1]})).toThrow(/input\.input/);
        }
    );
});

describe('executeActionInput', () => {
    const base = {actionName: 'create', bindingKey: 'crm', input: {}};

    it('omits componentVersion when absent', () => {
        expect(executeActionInput(base)).not.toHaveProperty('componentVersion');
    });

    it.each([-1, 1.5, '2', true])('rejects componentVersion %j', (componentVersion) => {
        expect(() => executeActionInput({...base, componentVersion})).toThrow(/componentVersion/);
    });
});

describe('proxyInput', () => {
    const base = {bindingKey: 'crm', request: {method: 'GET', path: '/f'}};

    it.each(['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'])('accepts method %s', (method) => {
        expect(proxyInput({...base, request: {method, path: '/f'}}).request.method).toBe(method);
    });

    it.each(['OPTIONS', 'get', 'TRACE', 5])('rejects method %j', (method) => {
        expect(() => proxyInput({...base, request: {method, path: '/f'}})).toThrow(/method/);
    });

    it.each(['f', '', 'https://x/f'])('rejects path %j not starting with /', (path) => {
        expect(() => proxyInput({...base, request: {method: 'GET', path}})).toThrow(/path/);
    });

    it('rejects a missing request', () => {
        expect(() => proxyInput({bindingKey: 'crm'})).toThrow(/request/);
    });

    it('passes headers, query, contentType and body through', () => {
        const request = {
            body: {a: 1},
            contentType: 'application/json',
            headers: {a: ['1', '2']},
            method: 'POST',
            path: '/f',
            query: {q: ['x']},
        };

        expect(proxyInput({bindingKey: 'crm', request}).request).toEqual(request);
    });

    it.each([['text'], [[1, 2]], [{a: 1}]])('accepts body %j', (body) => {
        expect(proxyInput({...base, request: {body, method: 'POST', path: '/f'}}).request.body).toEqual(body);
    });

    it.each([[5], [true], [new Uint8Array([1])]])('rejects body %j', (body) => {
        expect(() => proxyInput({...base, request: {body, method: 'POST', path: '/f'}})).toThrow(/body/);
    });

    it.each([
        ['headers', {a: 'x'}],
        ['headers', {a: [1]}],
        ['query', {a: 'x'}],
        ['headers', 'x'],
    ])('rejects malformed %s %j', (field, value) => {
        expect(() => proxyInput({...base, request: {...base.request, [field]: value}})).toThrow(new RegExp(field));
    });

    it('rejects a non-string contentType', () => {
        expect(() => proxyInput({...base, request: {...base.request, contentType: 5}})).toThrow(/contentType/);
    });
});

describe('executeWorkflowInput', () => {
    const base = {input: {}, workflowKey: 'wf'};

    it('accepts waitSeconds 0 and a positive integer', () => {
        expect(executeWorkflowInput({...base, waitSeconds: 0}).waitSeconds).toBe(0);
        expect(executeWorkflowInput({...base, waitSeconds: 30}).waitSeconds).toBe(30);
    });

    it.each([-1, 1.5, '3'])('rejects waitSeconds %j', (waitSeconds) => {
        expect(() => executeWorkflowInput({...base, waitSeconds})).toThrow(/waitSeconds/);
    });

    it('omits waitSeconds when absent', () => {
        expect(executeWorkflowInput(base)).not.toHaveProperty('waitSeconds');
    });

    it.each([true, false, 'approval', 'bogus', 1])('strips a wait key of %j', (wait) => {
        expect(executeWorkflowInput({...base, wait})).toStrictEqual(base);
    });
});

describe('plain objects only', () => {
    it.each(validators)('%s rejects a Map, a Date and a typed array as input', (_name, validator) => {
        expect(() => validator(new Map())).toThrow(TypeError);
        expect(() => validator(new Date())).toThrow(TypeError);
        expect(() => validator(new Uint16Array(2))).toThrow(TypeError);
    });

    it.each([
        ['executeActionInput', executeActionInput, {actionName: 'a', bindingKey: 'b'}],
        ['executeToolInput', executeToolInput, {bindingKey: 'b', toolName: 't'}],
        ['executeToolByNameInput', executeToolByNameInput, {name: 'n'}],
        ['executeWorkflowInput', executeWorkflowInput, {workflowKey: 'w'}],
    ] as Array<[string, (input: unknown) => unknown, Record<string, unknown>]>)(
        '%s rejects a Map, a Date and a typed array as input.input',
        (_name, validator, base) => {
            expect(() => validator({...base, input: new Map()})).toThrow(/input\.input/);
            expect(() => validator({...base, input: new Date()})).toThrow(/input\.input/);
            expect(() => validator({...base, input: new Uint16Array(2)})).toThrow(/input\.input/);
        }
    );

    it.each([new Map(), new Date(), new Uint16Array(2)])('proxyInput rejects %o as request', (request) => {
        expect(() => proxyInput({bindingKey: 'crm', request})).toThrow(/request/);
    });

    it.each([new Map(), new Date(), new Uint16Array(2)])('proxyInput rejects %o as request.body', (body) => {
        expect(() => proxyInput({bindingKey: 'crm', request: {body, method: 'POST', path: '/f'}})).toThrow(/body/);
    });

    it.each(['headers', 'query'])('proxyInput rejects a Map as %s', (field) => {
        expect(() => proxyInput({bindingKey: 'crm', request: {[field]: new Map(), method: 'GET', path: '/f'}})).toThrow(
            new RegExp(field)
        );
    });

    it.each(['headers', 'query'])('proxyInput keeps a __proto__ key in %s as an own property', (field) => {
        const map = JSON.parse('{"__proto__": ["a"], "b": ["c"]}') as Record<string, unknown>;
        const result = proxyInput({bindingKey: 'crm', request: {[field]: map, method: 'GET', path: '/f'}});
        const copy = (result.request as unknown as Record<string, Record<string, string[]>>)[field]!;

        expect(Object.getOwnPropertyDescriptor(copy, '__proto__')?.value).toEqual(['a']);
        expect(Object.getPrototypeOf(copy)).toBe(Object.prototype);
        expect(copy['b']).toEqual(['c']);
    });
});

describe('unknown keys', () => {
    it.each([
        ['executeActionInput', executeActionInput, {actionName: 'a', bindingKey: 'b', input: {x: 1}}],
        ['executeToolInput', executeToolInput, {bindingKey: 'b', input: {x: 1}, toolName: 't'}],
        ['executeToolByNameInput', executeToolByNameInput, {input: {x: 1}, name: 'n'}],
        ['proxyInput', proxyInput, {bindingKey: 'b', request: {method: 'GET', path: '/f'}}],
        ['executeWorkflowInput', executeWorkflowInput, {input: {x: 1}, workflowKey: 'w'}],
        ['getWorkflowRunInput', getWorkflowRunInput, {runId: 'r'}],
    ] as Array<[string, (input: unknown) => unknown, Record<string, unknown>]>)(
        '%s strips them from the result',
        (_name, validator, valid) => {
            expect(validator({...valid, extra: 1})).toStrictEqual(valid);
        }
    );

    it('proxyInput strips them from the request too', () => {
        const request = {method: 'GET', path: '/f'};

        expect(proxyInput({bindingKey: 'b', request: {...request, extra: 1}})).toStrictEqual({
            bindingKey: 'b',
            request,
        });
    });
});

describe('callApiInput', () => {
    it('rejects an unknown method', () => {
        expect(() => callApiInput({method: 'HEAD', path: 'v1/a/b'})).toThrow(/method/);
    });

    it('rejects a non-string path', () => {
        expect(() => callApiInput({method: 'GET', path: 1})).toThrow(/path/);
    });

    it('rejects a non-string header and a bad query value', () => {
        expect(() => callApiInput({headers: {a: 1}, method: 'GET', path: 'v1/a/b'})).toThrow(/headers/);
        expect(() => callApiInput({method: 'GET', path: 'v1/a/b', query: {a: 1}})).toThrow(/query/);
    });
});
