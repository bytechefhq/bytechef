import {describe, expect, it, vi} from 'vitest';

import pickChangedValues from '../pickChangedValues';

describe('pickChangedValues', () => {
    it('keeps only the top-level values that differ from their initial values', () => {
        expect(pickChangedValues({a: 1, b: 2, c: 4}, {a: 1, b: 2, c: 3})).toEqual({c: 4});
    });

    it('omits unchanged nested defaults', () => {
        expect(
            pickChangedValues({body: {content: 'x', type: 'JSON'}}, {body: {content: '', type: 'JSON'}})
        ).toStrictEqual({body: {content: 'x'}});
    });

    it('captures a deleted array item', () => {
        expect(pickChangedValues({tags: ['a']}, {tags: ['a', 'b']})).toStrictEqual({tags: ['a']});
    });

    it('takes a whole array when one of its items changes', () => {
        expect(pickChangedValues({tags: ['a', 'c']}, {tags: ['a', 'b']})).toStrictEqual({tags: ['a', 'c']});
    });

    it('omits an array equal to its initial value', () => {
        expect(pickChangedValues({tags: [{name: 'a'}]}, {tags: [{name: 'a'}]})).toStrictEqual({});
    });

    it('captures a removed object key', () => {
        expect(pickChangedValues({headers: {accept: 'json'}}, {headers: {accept: 'json', token: 'x'}})).toStrictEqual({
            headers: {accept: 'json', token: undefined},
        });
    });

    it('captures a cleared field', () => {
        expect(pickChangedValues({owner: ''}, {owner: 'me'})).toStrictEqual({owner: ''});
    });

    it('captures an ai fill written over an empty field', () => {
        expect(pickChangedValues({owner: '=fromAi(owner)'}, {owner: ''})).toStrictEqual({owner: '=fromAi(owner)'});
    });

    it('captures a field cleared from no initial value when the user changed it', () => {
        expect(pickChangedValues({owner: ''}, {}, () => true)).toStrictEqual({owner: ''});
    });

    it('treats an empty value that only came from field registration as unchanged', () => {
        expect(
            pickChangedValues({body: {format: ''}, labels: [], owner: '', repo: null}, {}, () => false)
        ).toStrictEqual({});
    });

    it('still captures a non-empty value written without a dirty mark', () => {
        expect(pickChangedValues({labels: ['bug']}, {}, () => false)).toStrictEqual({labels: ['bug']});
    });

    it('captures an empty nested field whose parent the user changed as a whole', () => {
        expect(pickChangedValues({body: {content: '', type: 'JSON'}}, {}, (path) => path === 'body')).toStrictEqual({
            body: {content: '', type: 'JSON'},
        });
    });

    it('asks about the dotted path of a nested field', () => {
        const isFieldDirty = vi.fn().mockReturnValue(false);

        pickChangedValues({body: {format: ''}}, {}, isFieldDirty);

        expect(isFieldDirty.mock.calls).toEqual([['body'], ['body.format']]);
    });

    it('captures a removed top-level key', () => {
        expect(pickChangedValues({accept: 'json'}, {accept: 'json', token: 'x'})).toStrictEqual({token: undefined});
    });

    it('captures nested values added under an object that had no initial value', () => {
        expect(pickChangedValues({body: {content: 'x', type: 'JSON'}}, {body: {type: 'JSON'}})).toStrictEqual({
            body: {content: 'x'},
        });
    });
});
