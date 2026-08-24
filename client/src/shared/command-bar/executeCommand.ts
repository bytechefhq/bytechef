import {
    type CommandActionType,
    type CommandContextI,
    type CommandI,
    type CommandRunContextI,
} from '@/shared/command-bar/types';
import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {type NavigateFunction} from 'react-router-dom';

export interface ExecuteCommandOptionsI {
    closePalette: () => void;
    context: CommandContextI;
    navigate: NavigateFunction;
    onError: (error: unknown) => void;
    recordRecent: (command: CommandI) => void;
}

async function runAction(action: CommandActionType, runContext: CommandRunContextI): Promise<void> {
    switch (action.type) {
        case 'callback':
            await action.run(runContext);

            return;
        case 'intent':
            useCommandIntentStore.getState().publish(action.key, action.payload);

            return;
        case 'navigate':
            await runContext.navigate(action.to);

            return;
    }
}

/**
 * Runs a command's actions in order, awaiting each. The palette closes first so a dialog opened by a later action does
 * not mount while the command dialog's own overlay is still unmounting.
 */
export async function executeCommand(command: CommandI, options: ExecuteCommandOptionsI): Promise<void> {
    const {closePalette, context, navigate, onError, recordRecent} = options;

    closePalette();

    const runContext: CommandRunContextI = {command, context, navigate};

    try {
        for (const action of command.actions ?? []) {
            await runAction(action, runContext);
        }
    } catch (error) {
        onError(error);

        return;
    }

    recordRecent(command);
}
