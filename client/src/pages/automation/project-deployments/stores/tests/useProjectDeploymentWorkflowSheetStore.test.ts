import {Workflow} from '@/shared/middleware/automation/configuration';
import {beforeEach, describe, expect, it} from 'vitest';

import useProjectDeploymentWorkflowSheetStore from '../useProjectDeploymentWorkflowSheetStore';

describe('useProjectDeploymentWorkflowSheetStore', () => {
    beforeEach(() => {
        useProjectDeploymentWorkflowSheetStore.setState({
            projectDeploymentId: undefined,
            projectDeploymentWorkflowSheetOpen: false,
            projectName: undefined,
            projectVersion: undefined,
            workflow: undefined,
        });
    });

    it('opens the sheet for a deployment, project and workflow in one update', () => {
        const workflow = {id: 'workflow1', label: 'workflow1'} as Workflow;

        useProjectDeploymentWorkflowSheetStore.getState().openProjectDeploymentWorkflowSheet({
            projectDeploymentId: 3,
            projectName: 'Subflow',
            projectVersion: 2,
            workflow,
        });

        const state = useProjectDeploymentWorkflowSheetStore.getState();

        expect(state.projectDeploymentId).toBe(3);
        expect(state.projectDeploymentWorkflowSheetOpen).toBe(true);
        expect(state.projectName).toBe('Subflow');
        expect(state.projectVersion).toBe(2);
        expect(state.workflow).toBe(workflow);
    });

    it('replaces a previously opened deployment and workflow', () => {
        const {openProjectDeploymentWorkflowSheet} = useProjectDeploymentWorkflowSheetStore.getState();

        openProjectDeploymentWorkflowSheet({
            projectDeploymentId: 3,
            projectName: 'Subflow',
            workflow: {id: 'workflow1'} as Workflow,
        });

        openProjectDeploymentWorkflowSheet({projectDeploymentId: 4, workflow: {id: 'workflow2'} as Workflow});

        const state = useProjectDeploymentWorkflowSheetStore.getState();

        expect(state.projectDeploymentId).toBe(4);
        expect(state.projectName).toBeUndefined();
        expect(state.projectVersion).toBeUndefined();
        expect(state.workflow?.id).toBe('workflow2');
    });

    it('closes the sheet but keeps the selection', () => {
        useProjectDeploymentWorkflowSheetStore.getState().openProjectDeploymentWorkflowSheet({
            projectDeploymentId: 3,
            workflow: {id: 'workflow1'} as Workflow,
        });

        useProjectDeploymentWorkflowSheetStore.getState().setProjectDeploymentWorkflowSheetOpen(false);

        const state = useProjectDeploymentWorkflowSheetStore.getState();

        expect(state.projectDeploymentWorkflowSheetOpen).toBe(false);
        expect(state.projectDeploymentId).toBe(3);
        expect(state.workflow?.id).toBe('workflow1');
    });
});
