import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {expect, it} from 'vitest';

import {getPathAfterWorkflowDelete} from '../getPathAfterWorkflowDelete';

type AutomationProjectWorkflowTemplateType =
    AutomationProjectsQuery['automationProjects'][number]['workflowTemplates'][number];

const workflowTemplates = [
    {workflowUuid: 'workflow-1'},
    {workflowUuid: 'workflow-2'},
] as AutomationProjectWorkflowTemplateType[];

it('opens the first workflow that remains in the project', () => {
    expect(getPathAfterWorkflowDelete(workflowTemplates, 'workflow-1')).toBe('/embedded/automations/workflow-2/editor');
    expect(getPathAfterWorkflowDelete(workflowTemplates, 'workflow-2')).toBe('/embedded/automations/workflow-1/editor');
});

it('falls back to the automations list when no workflow remains', () => {
    expect(getPathAfterWorkflowDelete(workflowTemplates.slice(0, 1), 'workflow-1')).toBe('/embedded/automations');
});
