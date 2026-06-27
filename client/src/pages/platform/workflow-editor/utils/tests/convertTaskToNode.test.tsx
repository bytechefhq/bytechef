import {ComponentDefinitionBasic, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {convertTaskToNode} from '../layoutUtils';

const definition = {icon: '<svg/>', name: 'webhook', title: 'Webhook'} as ComponentDefinitionBasic;

describe('convertTaskToNode', () => {
    it('marks the node as a trigger when isTrigger is true', () => {
        const task = {name: 'trigger_2', type: 'webhook/v1/onReceive'} as WorkflowTask;

        const node = convertTaskToNode(task, definition, true);

        expect(node.data.trigger).toBe(true);
        expect(node.id).toBe('trigger_2');
    });

    it('marks the node as a non-trigger when isTrigger is false', () => {
        const task = {name: 'action_1', type: 'logger/v1/info'} as WorkflowTask;

        const node = convertTaskToNode(task, definition, false);

        expect(node.data.trigger).toBe(false);
    });
});
