import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import {describe, expect, it} from 'vitest';

describe('getAgentPath', () => {
    it('builds the project-scoped agent route', () => {
        expect(getAgentPath({id: '22', projectId: '7'})).toBe('/automation/projects/7/agents/22');
    });
});
