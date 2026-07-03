import {describe, expect, it} from 'vitest';

import {toToolResultDataPart} from '../toToolResultDataPart';

describe('toToolResultDataPart', () => {
    it('maps select-property-option (selectPropertyOption + selectTriggerPropertyOption)', () => {
        const payload = JSON.stringify({
            componentName: 'slack',
            kind: 'select-property-option',
            options: [{label: 'general', value: 'C1'}],
            propertyName: 'channel',
            truncated: false,
        });

        const a = toToolResultDataPart('selectPropertyOption', payload);
        const b = toToolResultDataPart('selectTriggerPropertyOption', payload);

        expect(a?.ok).toBe(true);
        expect(b?.ok).toBe(true);

        if (a?.ok) {
            expect(a.type).toBe('data-select-property-option');
            expect(a.data).toEqual({
                componentName: 'slack',
                kind: 'select-property-option',
                options: [{label: 'general', value: 'C1'}],
                propertyName: 'channel',
                truncated: false,
            });
        }
    });

    it('maps select-connection', () => {
        const r = toToolResultDataPart(
            'selectConnection',
            JSON.stringify({componentLabel: 'Slack', componentName: 'slack', kind: 'select-connection'})
        );

        expect(r?.ok).toBe(true);

        if (r?.ok) {
            expect(r.type).toBe('data-select-connection');
            expect(r.data).toEqual({
                componentLabel: 'Slack',
                componentName: 'slack',
                kind: 'select-connection',
            });
        }
    });

    it('maps ask-user-question', () => {
        const r = toToolResultDataPart(
            'askUserQuestion',
            JSON.stringify({
                awaitingAnswer: true,
                kind: 'ask-user-question',
                questions: [{multiSelect: false, options: [], question: 'Q?'}],
            })
        );

        expect(r?.ok).toBe(true);

        if (r?.ok) {
            expect(r.type).toBe('data-ask-user-question');
            expect(r.data).toEqual({
                awaitingAnswer: true,
                kind: 'ask-user-question',
                questions: [{multiSelect: false, options: [], question: 'Q?'}],
            });
        }
    });

    it('maps create-connection', () => {
        const r = toToolResultDataPart(
            'createConnection',
            JSON.stringify({
                componentLabel: 'Slack',
                componentName: 'slack',
                kind: 'create-connection',
                suggestedName: 'My Slack',
            })
        );

        expect(r?.ok).toBe(true);

        if (r?.ok) {
            expect(r.type).toBe('data-create-connection');
            expect(r.data).toEqual({
                componentLabel: 'Slack',
                componentName: 'slack',
                kind: 'create-connection',
                suggestedName: 'My Slack',
            });
        }
    });

    it('returns an error result for a malformed payload (unparseable JSON)', () => {
        const r = toToolResultDataPart('selectPropertyOption', 'not json{');

        expect(r?.ok).toBe(false);

        if (r && !r.ok) {
            expect(r.toolName).toBe('selectPropertyOption');
            expect(r.errorMessage).toMatch(/unparseable/i);
        }
    });

    it('returns an error result when kind is wrong for selectConnection', () => {
        const r = toToolResultDataPart('selectConnection', JSON.stringify({componentName: 'slack', kind: 'nope'}));

        expect(r?.ok).toBe(false);

        if (r && !r.ok) {
            expect(r.toolName).toBe('selectConnection');
            expect(r.errorMessage).toMatch(/malformed/i);
        }
    });

    it('returns an error result when options is missing for selectPropertyOption', () => {
        const r = toToolResultDataPart(
            'selectPropertyOption',
            JSON.stringify({componentName: 'slack', kind: 'select-property-option', propertyName: 'channel'})
        );

        expect(r?.ok).toBe(false);

        if (r && !r.ok) {
            expect(r.errorMessage).toMatch(/malformed/i);
        }
    });

    it('returns an error result when questions is missing for askUserQuestion', () => {
        const r = toToolResultDataPart('askUserQuestion', JSON.stringify({kind: 'ask-user-question'}));

        expect(r?.ok).toBe(false);

        if (r && !r.ok) {
            expect(r.errorMessage).toMatch(/malformed/i);
        }
    });

    it('returns an error result when kind is missing for createConnection', () => {
        const r = toToolResultDataPart(
            'createConnection',
            JSON.stringify({componentLabel: 'Slack', componentName: 'slack'})
        );

        expect(r?.ok).toBe(false);

        if (r && !r.ok) {
            expect(r.toolName).toBe('createConnection');
            expect(r.errorMessage).toMatch(/malformed/i);
        }
    });

    it('returns undefined for an unhandled tool name', () => {
        expect(toToolResultDataPart('someOtherTool', '{}')).toBeUndefined();
    });

    it('returns undefined for openFileTab (not an interactive tool)', () => {
        expect(
            toToolResultDataPart('openFileTab', JSON.stringify({fileId: '1', name: 'f', opened: true}))
        ).toBeUndefined();
    });
});
