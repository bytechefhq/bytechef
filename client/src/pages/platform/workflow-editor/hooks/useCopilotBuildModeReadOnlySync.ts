import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useEffect} from 'react';

import {useWorkflowEditorReadOnly} from '../providers/workflowEditorReadOnlyContext';

export default function useCopilotBuildModeReadOnlySync(): void {
    const setBuildModeDisabled = useCopilotPanelStore((state) => state.setBuildModeDisabled);

    const readOnly = useWorkflowEditorReadOnly();

    useEffect(() => {
        setBuildModeDisabled(readOnly);

        if (readOnly) {
            const {context, setContext} = useCopilotStore.getState();

            if (context.mode === MODE.BUILD) {
                setContext({...context, mode: MODE.ASK});
            }
        }

        return () => setBuildModeDisabled(false);
    }, [readOnly, setBuildModeDisabled]);
}
