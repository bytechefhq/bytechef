import AiAgentHeader from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/AiAgentHeader';
import {AiAgentConfigurationPanel} from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/AiAgentConfigurationPanel';
import AiAgentTestingPanel from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel';
import {DataPillPanelSkeleton} from '@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {Suspense, lazy, useCallback} from 'react';
import {twMerge} from 'tailwind-merge';

const DataPillPanel = lazy(() => import('@/pages/platform/workflow-editor/components/datapills/DataPillPanel'));

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
    const aiAgentNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore((state) => state.aiAgentNodeDetailsPanelOpen);
    const currentNodeClusterElementType = useWorkflowNodeDetailsPanelStore(
        (state) => state.currentNode?.clusterElementType
    );
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const handleNodeDetailsPanelClose = useCallback(() => {
        useWorkflowNodeDetailsPanelStore.getState().setAiAgentNodeDetailsPanelOpen(false);
    }, []);

    const showNodeDetailsPanel =
        aiAgentNodeDetailsPanelOpen &&
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

                <div className="relative mb-4">
                    <div className="size-full overflow-hidden">
                        <AiAgentTestingPanel />
                    </div>

                    {showNodeDetailsPanel && (
                        <>
                            {dataPillPanelOpen && (
                                <div className="absolute inset-y-0 -left-[405px] z-10 w-[400px] overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background shadow-lg">
                                    <Suspense
                                        fallback={
                                            <DataPillPanelSkeleton className="relative inset-auto z-0 size-full max-w-none rounded-none border-0" />
                                        }
                                    >
                                        <DataPillPanel
                                            className="relative inset-auto z-0 size-full max-w-none animate-none rounded-none border-0"
                                            previousComponentDefinitions={previousComponentDefinitions}
                                            workflowNodeOutputs={workflowNodeOutputs}
                                        />
                                    </Suspense>
                                </div>
                            )}

                            <div className="absolute inset-y-0 left-0 w-[460px] overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background shadow-lg">
                                <WorkflowNodeDetailsPanel
                                    className="relative inset-auto z-0 size-full max-w-none rounded-none border-0"
                                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                                    onClose={handleNodeDetailsPanelClose}
                                    panelOpen
                                    previousComponentDefinitions={previousComponentDefinitions}
                                    updateWorkflowMutation={updateWorkflowMutation}
                                    workflowNodeOutputs={workflowNodeOutputs}
                                />
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
