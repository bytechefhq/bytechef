import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {type CommandI} from '@/shared/command-bar/types';
import {useRegisterCommands} from '@/shared/command-bar/useRegisterCommands';
import {CodeIcon, PanelBottomIcon, SettingsIcon, SquareArrowDownIcon, SquareArrowUpIcon} from 'lucide-react';
import {useMemo} from 'react';

const GROUP = 'Workflow Editor';

/**
 * Page-scoped commands: they exist only while the editor is mounted. Every action is a callback, so none of them are
 * recorded in recents -- replaying "toggle the bottom panel" from another page would be meaningless.
 */
export function useRegisterWorkflowEditorCommands(): void {
    const commands = useMemo<CommandI[]>(() => {
        const {
            setShowBottomPanelOpen,
            setShowEditWorkflowDialog,
            setShowWorkflowCodeEditorSheet,
            setShowWorkflowInputsSheet,
            setShowWorkflowOutputsSheet,
        } = useWorkflowEditorStore.getState();

        return [
            {
                actions: [{run: () => setShowWorkflowInputsSheet(true), type: 'callback'}],
                group: GROUP,
                icon: SquareArrowDownIcon,
                id: 'workflowEditor.inputs',
                title: 'Edit workflow inputs',
            },
            {
                actions: [{run: () => setShowWorkflowOutputsSheet(true), type: 'callback'}],
                group: GROUP,
                icon: SquareArrowUpIcon,
                id: 'workflowEditor.outputs',
                title: 'Edit workflow outputs',
            },
            {
                actions: [{run: () => setShowWorkflowCodeEditorSheet(true), type: 'callback'}],
                group: GROUP,
                icon: CodeIcon,
                id: 'workflowEditor.source',
                title: 'Open workflow source',
            },
            {
                actions: [{run: () => setShowEditWorkflowDialog(true), type: 'callback'}],
                group: GROUP,
                icon: SettingsIcon,
                id: 'workflowEditor.settings',
                title: 'Edit workflow settings',
            },
            {
                actions: [
                    {
                        run: () => setShowBottomPanelOpen(!useWorkflowEditorStore.getState().showBottomPanel),
                        type: 'callback',
                    },
                ],
                group: GROUP,
                icon: PanelBottomIcon,
                id: 'workflowEditor.bottomPanel',
                title: 'Toggle bottom panel',
            },
        ];
    }, []);

    useRegisterCommands(commands, [commands]);
}
