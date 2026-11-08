import {describe, expect, it} from 'vitest';

import {groupWorkflowsByProject} from '../groupWorkflowsByProject';

describe('groupWorkflowsByProject', () => {
    it('groups workflows by project, preserving first-seen project order', () => {
        const workflows = [
            {id: 'a', name: 'A', projectId: 'p1', projectName: 'Project One'},
            {id: 'b', name: 'B', projectId: 'p2', projectName: 'Project Two'},
            {id: 'c', name: 'C', projectId: 'p1', projectName: 'Project One'},
        ];

        const groups = groupWorkflowsByProject(workflows);

        expect(groups).toHaveLength(2);
        expect(groups[0]!.projectId).toBe('p1');
        expect(groups[0]!.projectName).toBe('Project One');
        expect(groups[0]!.workflows.map((workflow) => workflow.id)).toEqual(['a', 'c']);
        expect(groups[1]!.projectId).toBe('p2');
        expect(groups[1]!.workflows.map((workflow) => workflow.id)).toEqual(['b']);
    });

    it('returns an empty array for no workflows', () => {
        expect(groupWorkflowsByProject([])).toEqual([]);
    });
});
