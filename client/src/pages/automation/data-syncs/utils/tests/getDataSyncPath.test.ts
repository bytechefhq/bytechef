import getDataSyncPath from '@/pages/automation/data-syncs/utils/getDataSyncPath';
import {describe, expect, it} from 'vitest';

describe('getDataSyncPath', () => {
    it('builds the project-scoped data sync route', () => {
        expect(getDataSyncPath({id: '22', projectId: '7'})).toBe('/automation/projects/7/data-syncs/22');
    });
});
