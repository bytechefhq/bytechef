import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {useCallback} from 'react';

export default function useAiAgentEvals() {
    const {evalsTab, setEvalsPanelOpen, setEvalsTab} = useAiAgentEvalsStore();

    const handleClose = useCallback(() => {
        setEvalsPanelOpen(false);
    }, [setEvalsPanelOpen]);

    return {evalsTab, handleClose, setEvalsTab};
}
