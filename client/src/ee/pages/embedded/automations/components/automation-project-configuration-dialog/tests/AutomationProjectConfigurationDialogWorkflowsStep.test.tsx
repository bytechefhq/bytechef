import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AutomationProjectConfigurationDialogWorkflowsStep from '../AutomationProjectConfigurationDialogWorkflowsStep';
import {ConfigurationDialogWorkflowI} from '../automationProjectConfigurationDialog-utils';

vi.mock('../AutomationProjectConfigurationDialogWorkflowsStepItem', () => ({
    default: ({workflow}: {workflow: ConfigurationDialogWorkflowI}) => <div>{`workflow:${workflow.label}`}</div>,
}));

describe('AutomationProjectConfigurationDialogWorkflowsStep', () => {
    it('lists workflows alphabetically by label', () => {
        const workflows: ConfigurationDialogWorkflowI[] = [
            {enabled: true, label: 'Setup leads sheet', workflowUuid: 'setup-uuid'},
            {enabled: true, label: 'Connection check', workflowUuid: 'connection-uuid'},
            {enabled: true, label: 'archive old leads', workflowUuid: 'archive-uuid'},
        ];

        render(
            <AutomationProjectConfigurationDialogWorkflowsStep
                isLoading={false}
                onWorkflowDraftChange={vi.fn()}
                workflowDrafts={{}}
                workflows={workflows}
            />
        );

        const renderedLabels = screen.getAllByText(/^workflow:/).map((element) => element.textContent);

        expect(renderedLabels).toEqual([
            'workflow:archive old leads',
            'workflow:Connection check',
            'workflow:Setup leads sheet',
        ]);
    });
});
