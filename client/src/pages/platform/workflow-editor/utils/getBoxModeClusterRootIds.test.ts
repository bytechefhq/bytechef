import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getBoxModeClusterRootIds from './getBoxModeClusterRootIds';

const TASKS = [
    {clusterRoot: true, name: 'aiAgent_1', type: 'aiAgent/v1/chat'},
    {clusterElements: {}, name: 'condition_1', type: 'condition/v1'},
    {name: 'logger_1', type: 'logger/v1/info'},
] as Array<WorkflowTask>;

const TRIGGERS = [
    {clusterRoot: true, name: 'trigger_1', type: 'browser/v1/voiceSession'},
    {name: 'trigger_2', type: 'webhook/v1/onReceive'},
] as Array<WorkflowTrigger>;

describe('getBoxModeClusterRootIds', () => {
    it('returns no roots outside box mode', () => {
        expect(getBoxModeClusterRootIds({clusterElementsViewMode: 'dialog', tasks: TASKS, triggers: TRIGGERS})).toEqual(
            []
        );
    });

    it('boxes the cluster-root triggers as well as the cluster-root tasks', () => {
        expect(getBoxModeClusterRootIds({clusterElementsViewMode: 'box', tasks: TASKS, triggers: TRIGGERS})).toEqual([
            'trigger_1',
            'aiAgent_1',
        ]);
    });

    it('leaves out a collapsed root, trigger or task', () => {
        expect(
            getBoxModeClusterRootIds({
                clusterElementsViewMode: 'box',
                collapsedClusterRootIds: {trigger_1: true},
                tasks: TASKS,
                triggers: TRIGGERS,
            })
        ).toEqual(['aiAgent_1']);
    });

    it('tolerates missing tasks and triggers', () => {
        expect(getBoxModeClusterRootIds({clusterElementsViewMode: 'box'})).toEqual([]);
    });
});
