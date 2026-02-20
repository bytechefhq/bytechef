import AiAgentHeader from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/AiAgentHeader';
import {AiAgentConfigurationPanel} from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/AiAgentConfigurationPanel';
import AiAgentTestingPanel from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {twMerge} from 'tailwind-merge';

interface AiAgentEditorProps {
    className?: string;
    copilotEnabled?: boolean;
    onClose?: () => void;
    onCopilotClick?: () => void;
    onToggleEditor?: (showAiAgent: boolean) => void;
    previousComponentDefinitions?: ComponentDefinitionBasic[];
    workflowNodeOutputs?: WorkflowNodeOutput[];
}

export default function AiAgentEditor({
    className,
    copilotEnabled,
    onClose,
    onCopilotClick,
    onToggleEditor,
    previousComponentDefinitions,
    workflowNodeOutputs,
}: AiAgentEditorProps) {
    const currentNodeClusterElementType = useWorkflowNodeDetailsPanelStore(
        (state) => state.currentNode?.clusterElementType
    );
    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const showNodeDetailsPanel =
        workflowNodeDetailsPanelOpen &&
        (currentNodeClusterElementType === 'tools' || currentNodeClusterElementType === 'model') &&
        previousComponentDefinitions &&
        workflowNodeOutputs;

    return (
        <div className={twMerge('flex h-full flex-1 flex-col rounded-lg bg-white', className)}>
            <AiAgentHeader
                copilotEnabled={copilotEnabled}
                onClose={onClose}
                onCopilotClick={onCopilotClick}
                onToggleEditor={onToggleEditor}
            />

            <div className="grid min-h-0 flex-1 grid-cols-2 gap-6 overflow-hidden px-4">
                <div className="overflow-y-auto">
                    <AiAgentConfigurationPanel />
                </div>

                <div className="relative mb-4 overflow-hidden">
                    <AiAgentTestingPanel />

                    {showNodeDetailsPanel && (
                        <div className="absolute inset-y-0 left-0 w-[460px] overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background shadow-lg">
                            <WorkflowNodeDetailsPanel
                                className="relative inset-auto z-0 size-full max-w-none rounded-none border-0"
                                invalidateWorkflowQueries={invalidateWorkflowQueries!}
                                previousComponentDefinitions={previousComponentDefinitions}
                                updateWorkflowMutation={updateWorkflowMutation}
                                workflowNodeOutputs={workflowNodeOutputs}
                            />
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
