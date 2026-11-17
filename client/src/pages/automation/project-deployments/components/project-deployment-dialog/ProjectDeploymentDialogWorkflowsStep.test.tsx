import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDialogWorkflowsStep from './ProjectDeploymentDialogWorkflowsStep';

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem',
    () => ({
        default: ({label, workflowIndex}: {label: string; workflowIndex: number}) => (
            <div>{`${label}:${workflowIndex}`}</div>
        ),
    })
);

describe('ProjectDeploymentDialogWorkflowsStep', () => {
    it('binds each workflow card to its entry in the form workflows array when a subflow comes first', () => {
        const workflows = [
            {id: 'subflow-id', label: 'subflow', tasks: [], triggers: [], workflowUuid: 'subflow-uuid'},
            {
                id: 'parent-id',
                label: 'parent',
                tasks: [{name: 'subflow_1', parameters: {workflowUuid: 'subflow-uuid'}, type: 'subflow/v1/call'}],
                triggers: [],
                workflowUuid: 'parent-uuid',
            },
            {id: 'other-id', label: 'other', tasks: [], triggers: [], workflowUuid: 'other-uuid'},
        ] as Workflow[];

        render(
            <ProjectDeploymentDialogWorkflowsStep
                control={{} as Control<ProjectDeployment>}
                formState={{} as FormState<ProjectDeployment>}
                setValue={vi.fn() as unknown as UseFormSetValue<ProjectDeployment>}
                workflows={workflows}
            />
        );

        expect(screen.queryByText(/^subflow:/)).not.toBeInTheDocument();
        expect(screen.getByText('parent:1')).toBeInTheDocument();
        expect(screen.getByText('other:2')).toBeInTheDocument();
    });

    it('lists workflows alphabetically by label while keeping each card bound to its form index', () => {
        const workflows = [
            {id: 'setup-id', label: 'Setup leads sheet', tasks: [], triggers: [], workflowUuid: 'setup-uuid'},
            {id: 'connection-id', label: 'Connection check', tasks: [], triggers: [], workflowUuid: 'connection-uuid'},
            {id: 'archive-id', label: 'archive old leads', tasks: [], triggers: [], workflowUuid: 'archive-uuid'},
            {id: 'unlabeled-id', tasks: [], triggers: [], workflowUuid: 'unlabeled-uuid'},
        ] as Workflow[];

        render(
            <ProjectDeploymentDialogWorkflowsStep
                control={{} as Control<ProjectDeployment>}
                formState={{} as FormState<ProjectDeployment>}
                setValue={vi.fn() as unknown as UseFormSetValue<ProjectDeployment>}
                workflows={workflows}
            />
        );

        const renderedLabels = screen.getAllByText(/:\d+$/).map((element) => element.textContent);

        expect(renderedLabels).toEqual([
            'undefined:3',
            'archive old leads:2',
            'Connection check:1',
            'Setup leads sheet:0',
        ]);
    });
});
