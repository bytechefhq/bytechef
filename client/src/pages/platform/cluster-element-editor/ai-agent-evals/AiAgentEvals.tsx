import EvalsJudgesTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/judges/EvalsJudgesTab';
import EvalsRunsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/EvalsRunsTab';
import EvalsTestsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/EvalsTestsTab';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {twMerge} from 'tailwind-merge';

const AiAgentEvals = () => {
    const {evalsTab, setEvalsTab} = useAiAgentEvalsStore();

    const workflow = useWorkflowDataStore((state) => state.workflow);
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const workflowId = workflow.id ?? '';
    const workflowNodeName = rootClusterElementNodeData?.workflowNodeName ?? '';

    return (
        <div className="flex flex-1 flex-col">
            <div className="flex border-b border-b-border/50 px-4">
                {(['tests', 'judges', 'runs'] as const).map((tab) => (
                    <button
                        className={twMerge(
                            'px-4 py-2.5 text-sm',
                            evalsTab === tab
                                ? 'border-b-2 border-blue-500 font-semibold text-gray-900'
                                : 'text-gray-500'
                        )}
                        key={tab}
                        onClick={() => setEvalsTab(tab)}
                    >
                        {tab.charAt(0).toUpperCase() + tab.slice(1)}
                    </button>
                ))}
            </div>

            <div className="flex min-h-0 flex-1 flex-col overflow-y-auto py-4">
                {evalsTab === 'tests' && workflowId && workflowNodeName && (
                    <EvalsTestsTab workflowId={workflowId} workflowNodeName={workflowNodeName} />
                )}

                {evalsTab === 'judges' && workflowId && workflowNodeName && (
                    <EvalsJudgesTab workflowId={workflowId} workflowNodeName={workflowNodeName} />
                )}

                {evalsTab === 'runs' && <EvalsRunsTab />}
            </div>
        </div>
    );
};

export default AiAgentEvals;
