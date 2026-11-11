import {describe, expect, it} from 'vitest';

import buildToolParametersPayload from '../buildToolParametersPayload';

describe('buildToolParametersPayload', () => {
    it('merges saved ai and dirty values with dirty winning', () => {
        expect(
            buildToolParametersPayload({
                aiValues: {limit: '=fromAi(limit)', owner: '=fromAi(owner)'},
                dirtyValues: {owner: 'me'},
                savedParameters: {limit: 5, repo: 'x'},
            })
        ).toEqual({limit: '=fromAi(limit)', owner: 'me', repo: 'x'});
    });

    it('drops a saved nested key that the changed values mark as removed', () => {
        expect(
            buildToolParametersPayload({
                aiValues: {},
                dirtyValues: {headers: {accept: 'json', token: undefined}},
                savedParameters: {headers: {accept: 'json', token: 'x'}},
            })
        ).toStrictEqual({headers: {accept: 'json'}});
    });

    it('omits untouched defaults', () => {
        expect(buildToolParametersPayload({aiValues: {}, dirtyValues: {}, savedParameters: null})).toEqual({});
    });

    it('stores cleared field as null', () => {
        expect(
            buildToolParametersPayload({aiValues: {}, dirtyValues: {owner: ''}, savedParameters: {owner: 'me'}})
        ).toEqual({owner: null});
    });

    it('keeps saved nested siblings when one nested field changes', () => {
        expect(
            buildToolParametersPayload({
                aiValues: {},
                dirtyValues: {body: {content: 'new'}},
                savedParameters: {body: {content: 'old', type: 'JSON'}},
            })
        ).toEqual({body: {content: 'new', type: 'JSON'}});
    });

    it('applies dotted ai paths as nested values without dropping siblings', () => {
        expect(
            buildToolParametersPayload({
                aiValues: {'body.content': '=fromAi(content)'},
                dirtyValues: {},
                savedParameters: {body: {type: 'JSON'}},
            })
        ).toEqual({body: {content: '=fromAi(content)', type: 'JSON'}});
    });
});
