import {describe, expect, it, vi} from 'vitest';

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

    it('maps knowledge-base-citations', () => {
        const r = toToolResultDataPart(
            'queryKnowledgeBase',
            JSON.stringify({
                hits: [
                    {
                        docId: '7',
                        docTitle: 'Onboarding Guide',
                        excerpt: 'Spring AI supports vector stores.',
                        knowledgeBaseId: '42',
                        knowledgeBaseName: 'Company Docs',
                        score: 0.92,
                    },
                ],
                kind: 'knowledge-base-citations',
            })
        );

        expect(r?.ok).toBe(true);

        if (r?.ok) {
            expect(r.type).toBe('data-knowledge-base-citations');
            expect(r.data).toEqual({
                hits: [
                    {
                        docId: '7',
                        docTitle: 'Onboarding Guide',
                        excerpt: 'Spring AI supports vector stores.',
                        knowledgeBaseId: '42',
                        knowledgeBaseName: 'Company Docs',
                        score: 0.92,
                    },
                ],
                kind: 'knowledge-base-citations',
            });
        }
    });

    it('returns undefined for queryKnowledgeBase results without renderable hits', () => {
        // Empty hits mean "nothing to cite", and an {"error": ...} payload is handled by the model in prose —
        // neither should produce a data part (unlike the interactive tools, which surface an error result).
        expect(
            toToolResultDataPart('queryKnowledgeBase', JSON.stringify({hits: [], kind: 'knowledge-base-citations'}))
        ).toBeUndefined();
        expect(
            toToolResultDataPart('queryKnowledgeBase', JSON.stringify({error: 'question is required'}))
        ).toBeUndefined();
        expect(toToolResultDataPart('queryKnowledgeBase', 'not json{')).toBeUndefined();
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

describe('toToolResultDataPart payload-kind fallback', () => {
    it('renders an ask-user-question payload returned by a delegate tool', () => {
        const result = toToolResultDataPart(
            'personal_agent_manager',
            JSON.stringify({
                kind: 'ask-user-question',
                questions: [
                    {
                        header: 'Agent',
                        multiSelect: false,
                        options: [{description: 'the support agent', label: 'Support'}],
                        question: 'Which agent?',
                    },
                ],
            })
        );

        expect(result).toMatchObject({ok: true, type: 'data-ask-user-question'});
    });

    it('ignores an unknown kind', () => {
        const result = toToolResultDataPart('personal_agent_manager', JSON.stringify({kind: 'something-else'}));

        expect(result).toBeUndefined();
    });

    it('ignores a non-JSON result without throwing', () => {
        expect(() => toToolResultDataPart('personal_agent_manager', 'Created agent 7.')).not.toThrow();
        expect(toToolResultDataPart('personal_agent_manager', 'Created agent 7.')).toBeUndefined();
    });

    it('does not log a warning for an ordinary plain-text tool result', () => {
        const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        toToolResultDataPart('personal_agent_manager', 'Created agent 7.');

        expect(warnSpy).not.toHaveBeenCalled();

        warnSpy.mockRestore();
    });

    it('returns undefined for a tool result that is a JSON array', () => {
        expect(toToolResultDataPart('personal_agent_manager', '[1,2,3]')).toBeUndefined();
    });
});
