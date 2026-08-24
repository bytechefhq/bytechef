import {useRegisterWorkflowEditorCommands} from '@/pages/platform/workflow-editor/hooks/useRegisterWorkflowEditorCommands';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {type CommandActionType, type CommandContextI} from '@/shared/command-bar/types';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const context: CommandContextI = {
    edition: 'EE',
    featureFlags: () => true,
    pathname: '/automation/projects/1/project-workflows/2',
};

function runCallback(action: CommandActionType): Promise<void> | void {
    if (action.type !== 'callback') {
        throw new Error(`Expected a callback action, got "${action.type}".`);
    }

    return action.run({} as never);
}

describe('useRegisterWorkflowEditorCommands', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();

        useWorkflowEditorStore.setState({
            showBottomPanel: false,
            showEditWorkflowDialog: false,
            showWorkflowCodeEditorSheet: false,
            showWorkflowInputsSheet: false,
            showWorkflowOutputsSheet: false,
        });
    });

    it('is absent before the editor mounts', () => {
        expect(collectCommands(useCommandSourceRegistry.getState().sources, context)).toEqual([]);
    });

    it('registers exactly the five editor commands, in the Workflow Editor group, while mounted', () => {
        const {unmount} = renderHook(() => useRegisterWorkflowEditorCommands());

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands.map((command) => command.id).sort()).toEqual([
            'workflowEditor.bottomPanel',
            'workflowEditor.inputs',
            'workflowEditor.outputs',
            'workflowEditor.settings',
            'workflowEditor.source',
        ]);
        expect(commands.every((command) => command.group === 'Workflow Editor')).toBe(true);
        expect(commands.every((command) => command.actions?.length === 1)).toBe(true);
        expect(commands.every((command) => command.actions![0].type === 'callback')).toBe(true);

        unmount();

        expect(collectCommands(useCommandSourceRegistry.getState().sources, context)).toEqual([]);
    });

    it('opens the inputs sheet when its command runs', async () => {
        renderHook(() => useRegisterWorkflowEditorCommands());

        const command = collectCommands(useCommandSourceRegistry.getState().sources, context).find(
            (registered) => registered.id === 'workflowEditor.inputs'
        )!;

        expect(useWorkflowEditorStore.getState().showWorkflowInputsSheet).toBe(false);

        await runCallback(command.actions![0]);

        expect(useWorkflowEditorStore.getState().showWorkflowInputsSheet).toBe(true);
    });

    it('opens the outputs sheet when its command runs', async () => {
        renderHook(() => useRegisterWorkflowEditorCommands());

        const command = collectCommands(useCommandSourceRegistry.getState().sources, context).find(
            (registered) => registered.id === 'workflowEditor.outputs'
        )!;

        await runCallback(command.actions![0]);

        expect(useWorkflowEditorStore.getState().showWorkflowOutputsSheet).toBe(true);
    });

    it('opens the source editor sheet when its command runs', async () => {
        renderHook(() => useRegisterWorkflowEditorCommands());

        const command = collectCommands(useCommandSourceRegistry.getState().sources, context).find(
            (registered) => registered.id === 'workflowEditor.source'
        )!;

        await runCallback(command.actions![0]);

        expect(useWorkflowEditorStore.getState().showWorkflowCodeEditorSheet).toBe(true);
    });

    it('opens the edit workflow dialog when its command runs', async () => {
        renderHook(() => useRegisterWorkflowEditorCommands());

        const command = collectCommands(useCommandSourceRegistry.getState().sources, context).find(
            (registered) => registered.id === 'workflowEditor.settings'
        )!;

        await runCallback(command.actions![0]);

        expect(useWorkflowEditorStore.getState().showEditWorkflowDialog).toBe(true);
    });

    it('toggles the bottom panel open and closed on repeated runs', async () => {
        renderHook(() => useRegisterWorkflowEditorCommands());

        const command = collectCommands(useCommandSourceRegistry.getState().sources, context).find(
            (registered) => registered.id === 'workflowEditor.bottomPanel'
        )!;

        expect(useWorkflowEditorStore.getState().showBottomPanel).toBe(false);

        await runCallback(command.actions![0]);

        expect(useWorkflowEditorStore.getState().showBottomPanel).toBe(true);

        await runCallback(command.actions![0]);

        expect(useWorkflowEditorStore.getState().showBottomPanel).toBe(false);
    });
});
