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
