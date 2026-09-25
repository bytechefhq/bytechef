import {describe, expect, it} from 'vitest';

import getWorkflowCodeEditorIssueMessages from './getWorkflowCodeEditorIssueMessages';

const definition = JSON.stringify({
    tasks: [
        {
            name: 'fork-join_1',
            parameters: {
                branches: [
                    [{name: 'firecrawl_7', parameters: {}, type: 'firecrawl/v1/scrape'}],
                    [
                        {name: 'firecrawl_5', parameters: {}, type: 'firecrawl/v1/scrape'},
                        {
                            name: 'anthropic_2',
                            parameters: {
                                topK: '${firecrawl_5.data.json.result}',
                                userPrompt: '${ghost_1.data.html}',
                            },
                            type: 'anthropic/v1/ask',
                        },
                    ],
                ],
            },
            type: 'fork-join/v1',
        },
    ],
    triggers: [{name: 'trigger_1', type: 'webhook/v1/awaitWorkflowAndRespond'}],
});

const typeMismatchMessage =
    "Property 'firecrawl_5.data.json.result' in output of 'firecrawl/v1/scrape' is of type array, not integer";

describe('getWorkflowCodeEditorIssueMessages', () => {
    it("names the parameter of a validator issue and adds the editor's own issues of the draft", () => {
        expect(
            getWorkflowCodeEditorIssueMessages({
                definition,
                errors: [`[anthropic_2] ${typeMismatchMessage}`, 'Workflow has no label'],
                nodeIssues: [
                    {
                        kind: 'TYPE_MISMATCH',
                        message: typeMismatchMessage,
                        nodeName: 'anthropic_2',
                        propertyPath: 'firecrawl_5.data.json.result',
                        severity: 'ERROR',
                    },
                ],
                warnings: [],
            })
        ).toEqual({
            errors: [
                `[anthropic_2] topK: ${typeMismatchMessage}`,
                'Workflow has no label',
                '[anthropic_2] userPrompt: "ghost_1" is missing from the workflow (referenced as ghost_1.data.html)',
            ],
            warnings: [],
        });
    });

    it("lists an issue the validator already reports once, in the validator's words", () => {
        const duplicateMessage = 'Node names must be unique. Duplicate node name: logger_1';

        const {errors} = getWorkflowCodeEditorIssueMessages({
            definition: JSON.stringify({
                tasks: [
                    {name: 'logger_1', parameters: {}, type: 'logger/v1/info'},
                    {name: 'logger_1', parameters: {}, type: 'logger/v1/info'},
                ],
            }),
            errors: [duplicateMessage],
            nodeIssues: [
                {kind: 'DUPLICATE_NODE_NAME', message: duplicateMessage, nodeName: 'logger_1', severity: 'ERROR'},
            ],
            warnings: [],
        });

        expect(errors).toEqual([duplicateMessage]);
    });

    it('passes the validator messages through unchanged while the draft is not valid JSON', () => {
        expect(
            getWorkflowCodeEditorIssueMessages({
                definition: '{"tasks": [',
                errors: ['Invalid JSON'],
                nodeIssues: [],
                warnings: ['Something'],
            })
        ).toEqual({errors: ['Invalid JSON'], warnings: ['Something']});
    });

    it('names the parameter of a cluster element issue the validator reports under its cluster root', () => {
        const temperatureMessage = "Property 'openAi_1.temperature' has incorrect type. Expected number";

        const {errors} = getWorkflowCodeEditorIssueMessages({
            definition: JSON.stringify({
                tasks: [
                    {
                        clusterElements: {
                            model: {name: 'openAi_1', parameters: {temperature: 'hot'}, type: 'openAi/v1/model'},
                        },
                        name: 'aiAgent_1',
                        parameters: {},
                        type: 'aiAgent/v1/chat',
                    },
                ],
            }),
            errors: [`[aiAgent_1] ${temperatureMessage}`],
            nodeIssues: [
                {
                    kind: 'TYPE_MISMATCH',
                    message: temperatureMessage,
                    nodeName: 'aiAgent_1',
                    propertyPath: 'openAi_1.temperature',
                    severity: 'ERROR',
                },
            ],
            warnings: [],
        });

        expect(errors).toEqual([`[aiAgent_1] openAi_1.temperature: ${temperatureMessage}`]);
    });

    it('skips task and trigger entries that are not nodes instead of failing', () => {
        expect(
            getWorkflowCodeEditorIssueMessages({
                definition: JSON.stringify({
                    tasks: [null, {name: 'logger_1', parameters: {text: '${ghost_1.value}'}, type: 'logger/v1/info'}],
                    triggers: [null, 'trigger_1'],
                }),
                errors: [],
                nodeIssues: [],
                warnings: [],
            }).errors
        ).toEqual(['[logger_1] text: "ghost_1" is missing from the workflow (referenced as ghost_1.value)']);
    });

    it('falls back to the validator messages when a nested entry of the draft cannot be read', () => {
        expect(
            getWorkflowCodeEditorIssueMessages({
                definition: JSON.stringify({
                    tasks: [
                        {
                            name: 'condition_1',
                            parameters: {caseTrue: [{name: 'logger_1', type: 'logger/v1/info'}, null]},
                            type: 'condition/v1',
                        },
                    ],
                }),
                errors: ['[condition_1] Missing required property: expression'],
                nodeIssues: [],
                warnings: [],
            })
        ).toEqual({errors: ['[condition_1] Missing required property: expression'], warnings: []});
    });
});
