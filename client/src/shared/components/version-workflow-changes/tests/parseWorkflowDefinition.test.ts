import {describe, expect, it} from 'vitest';

import {collectComponentNames, parseWorkflowDefinition} from '../parseWorkflowDefinition';

describe('parseWorkflowDefinition', () => {
    it('parses label, description, tasks and triggers', () => {
        const workflow = parseWorkflowDefinition(
            JSON.stringify({
                description: 'A description',
                label: 'A label',
                tasks: [{name: 'http_1', type: 'http/v1/get'}],
                triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
            })
        );

        expect(workflow.label).toBe('A label');
        expect(workflow.description).toBe('A description');
        expect(workflow.tasks).toHaveLength(1);
        expect(workflow.triggers).toHaveLength(1);
    });

    it('defaults tasks and triggers to empty arrays', () => {
        const workflow = parseWorkflowDefinition('{"label":"Empty"}');

        expect(workflow.tasks).toEqual([]);
        expect(workflow.triggers).toEqual([]);
    });

    it('collects distinct component names including nested tasks and triggers', () => {
        const workflow = parseWorkflowDefinition(
            JSON.stringify({
                tasks: [
                    {
                        name: 'condition_1',
                        parameters: {
                            caseTrue: [{name: 'slack_1', type: 'slack/v1/sendMessage'}],
                            rawExpression: 'x',
                        },
                        type: 'condition/v1',
                    },
                    {name: 'slack_2', type: 'slack/v1/sendMessage'},
                    {name: 'http_1', type: 'http/v1/get'},
                ],
                triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
            })
        );

        expect(collectComponentNames(workflow).sort()).toEqual(['condition', 'http', 'manual', 'slack']);
    });
});
