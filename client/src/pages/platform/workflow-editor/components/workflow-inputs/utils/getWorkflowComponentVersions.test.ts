import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getWorkflowComponentVersions} from './getWorkflowComponentVersions';

describe('getWorkflowComponentVersions', () => {
    it('maps component names to versions from tasks and triggers', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [{type: 'slack/v2/sendMessage'}],
                triggers: [{type: 'webhook/v1/newMessage'}],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({slack: 2, webhook: 1});
    });

    it('returns an empty object when definition is missing', () => {
        const workflow = {} as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('returns an empty object when definition is unparseable', () => {
        const workflow = {definition: 'not json'} as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('skips node types without a v-prefixed version token', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [{type: 'slack/2/sendMessage'}, {type: 'logger'}],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('skips node types with a non-numeric version token', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [{type: 'slack/vX/sendMessage'}],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('skips nodes without a type', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'noType'}, {type: 'slack/v3/sendMessage'}],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({slack: 3});
    });

    it('returns an empty object when tasks is not an array', () => {
        const workflow = {
            definition: JSON.stringify({tasks: {type: 'slack/v2/sendMessage'}}),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });
});
