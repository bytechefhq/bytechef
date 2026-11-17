import {IntegrationInstanceConfiguration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialogWorkflowsStep from '../IntegrationInstanceConfigurationDialogWorkflowsStep';

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem',
    () => ({
        default: ({label, workflowIndex}: {label: string; workflowIndex: number}) => (
            <div>{`${label}:${workflowIndex}`}</div>
        ),
    })
);

describe('IntegrationInstanceConfigurationDialogWorkflowsStep', () => {
    it('lists workflows alphabetically by label while keeping each card bound to its form index', () => {
        const workflows = [
            {id: 'setup-id', label: 'Setup leads sheet'},
            {id: 'connection-id', label: 'Connection check'},
            {id: 'archive-id', label: 'archive old leads'},
        ] as Workflow[];

        render(
            <IntegrationInstanceConfigurationDialogWorkflowsStep
                componentName="googleSheets"
                control={{} as Control<IntegrationInstanceConfiguration>}
                formState={{} as FormState<IntegrationInstanceConfiguration>}
                setValue={vi.fn() as unknown as UseFormSetValue<IntegrationInstanceConfiguration>}
                workflows={workflows}
            />
        );

        const renderedLabels = screen.getAllByText(/:\d+$/).map((element) => element.textContent);

        expect(renderedLabels).toEqual(['archive old leads:2', 'Connection check:1', 'Setup leads sheet:0']);
    });
});
