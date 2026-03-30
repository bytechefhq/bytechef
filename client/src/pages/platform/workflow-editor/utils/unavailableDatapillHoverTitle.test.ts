import {describe, expect, it} from 'vitest';

import {
    buildUnavailableDataPillHoverTitle,
    getWorkflowNodeNameFromDataPillValue,
} from './unavailableDatapillHoverTitle';

import type {ComponentDefinitionBasic, Workflow} from '@/shared/middleware/platform/configuration';

describe('extractWorkflowNodeNameFromDatapillReference', () => {
    it('returns first path segment before dots', () => {
        expect(getWorkflowNodeNameFromDataPillValue('slack_1.body')).toBe('slack_1');
    });

    it('strips bracket suffix from first segment', () => {
        expect(getWorkflowNodeNameFromDataPillValue('items[0].id')).toBe('items');
    });

    it('returns undefined for empty string', () => {
        expect(getWorkflowNodeNameFromDataPillValue('')).toBeUndefined();
    });
});

describe('buildUnavailableDatapillHoverTitle', () => {
    const slackDefinition: ComponentDefinitionBasic = {
        name: 'slack',
        title: 'Slack',
        version: 1,
    };

    it('describes existing task with component title only', () => {
        const workflow = {
            tasks: [
                {
                    label: 'Post message',
                    name: 'slack_1',
                    type: 'slack/v1/sendMessage',
                },
            ],
        } as Workflow;

        const title = buildUnavailableDataPillHoverTitle({
            componentDefinitions: [slackDefinition],
            mentionId: 'slack_1.channel',
            taskDispatcherDefinitions: [],
            workflow,
        });

        expect(title).toContain('Slack');
        expect(title).not.toContain('sendMessage');
    });

    it('uses guessed component title when step is missing', () => {
        const workflow = {tasks: []} as Workflow;

        const title = buildUnavailableDataPillHoverTitle({
            componentDefinitions: [slackDefinition],
            mentionId: 'slack_1.channel',
            taskDispatcherDefinitions: [],
            workflow,
        });

        expect(title).toContain('Slack');
    });

    it('matches trigger by name', () => {
        const workflow = {
            tasks: [],
            triggers: [
                {
                    label: 'On schedule',
                    name: 'trigger_1',
                    type: 'schedule/v1/everyDay',
                },
            ],
        } as Workflow;

        const title = buildUnavailableDataPillHoverTitle({
            componentDefinitions: [],
            mentionId: 'trigger_1.timestamp',
            taskDispatcherDefinitions: [],
            workflow,
        });

        expect(title).toContain('schedule');
        expect(title).not.toContain('everyDay');
    });
});
