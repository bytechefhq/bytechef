import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getDataPillIconSource} from './getDataPillIconSource';

const workflow = {} as Workflow;

const taskDispatcherDefinitions = [
    {icon: 'fork-join-icon', name: 'fork-join', outputDefined: true, version: 1},
    {icon: 'on-error-icon', name: 'on-error', outputDefined: true, version: 1},
];

describe('getDataPillIconSource', () => {
    it('returns the dispatcher icon for a camelCased dispatcher node name', () => {
        expect(
            getDataPillIconSource({
                mentionDisplay: '${forkJoin_1.branch_0.result}',
                taskDispatcherDefinitions,
                workflow,
            })
        ).toBe('fork-join-icon');
        expect(getDataPillIconSource({mentionDisplay: '${onError_2}', taskDispatcherDefinitions, workflow})).toBe(
            'on-error-icon'
        );
    });

    it('returns the dispatcher icon for a hyphenated dispatcher node name', () => {
        expect(
            getDataPillIconSource({mentionDisplay: '${fork-join_1.branch_0}', taskDispatcherDefinitions, workflow})
        ).toBe('fork-join-icon');
    });

    it('resolves the component from the workflow definition when the node has a custom name', () => {
        const customNameWorkflow = {
            tasks: [{name: 'config', type: 'var/v1/set'}],
        } as Workflow;

        expect(
            getDataPillIconSource({
                componentDefinitions: [{icon: 'var-icon', name: 'var', version: 1}],
                mentionDisplay: '${config.batches}',
                workflow: customNameWorkflow,
            })
        ).toBe('var-icon');
    });

    it('resolves a custom-named node nested inside a task dispatcher', () => {
        const nestedWorkflow = {
            tasks: [
                {
                    name: 'loop_1',
                    parameters: {
                        iteratee: [{name: 'customer', type: 'postgresql/v1/query'}],
                    },
                    type: 'loop/v1',
                },
            ],
        } as unknown as Workflow;

        expect(
            getDataPillIconSource({
                componentDefinitions: [{icon: 'postgresql-icon', name: 'postgresql', version: 1}],
                mentionDisplay: '${customer[0].email}',
                workflow: nestedWorkflow,
            })
        ).toBe('postgresql-icon');
    });

    it('resolves a custom-named trigger from the workflow definition', () => {
        const triggerWorkflow = {
            triggers: [{name: 'start', type: 'manual/v1/manual'}],
        } as Workflow;

        expect(
            getDataPillIconSource({
                componentDefinitions: [{icon: 'manual-icon', name: 'manual', version: 1}],
                mentionDisplay: '${start.payload}',
                workflow: triggerWorkflow,
            })
        ).toBe('manual-icon');
    });

    it('prefers the definition type over the node name prefix', () => {
        const renamedWorkflow = {
            tasks: [{name: 'httpClient_1', type: 'postgresql/v1/query'}],
        } as Workflow;

        expect(
            getDataPillIconSource({
                componentDefinitions: [
                    {icon: 'http-client-icon', name: 'httpClient', version: 1},
                    {icon: 'postgresql-icon', name: 'postgresql', version: 1},
                ],
                mentionDisplay: '${httpClient_1.body}',
                workflow: renamedWorkflow,
            })
        ).toBe('postgresql-icon');
    });

    it('returns the component icon for a component node name', () => {
        expect(
            getDataPillIconSource({
                componentDefinitions: [{icon: 'http-client-icon', name: 'httpClient', version: 1}],
                mentionDisplay: '${httpClient_1.body}',
                workflow,
            })
        ).toBe('http-client-icon');
    });
});
