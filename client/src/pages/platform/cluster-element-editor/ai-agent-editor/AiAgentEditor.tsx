import AiAgentHeader from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/AiAgentHeader';
import {AiAgentConfigurationPanel} from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/AiAgentConfigurationPanel';
import AiAgentTestingPanel from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel';
import useAiAgentEditor from '@/pages/platform/cluster-element-editor/ai-agent-editor/hooks/useAiAgentEditor';
import AiAgentEvals from '@/pages/platform/cluster-element-editor/ai-agent-evals/AiAgentEvals';
import useAgentEvals from '@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAgentEvals';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import AiAgentSkills from '@/pages/platform/cluster-element-editor/ai-agent-skills/AiAgentSkills';
import useAgentSkills from '@/pages/platform/cluster-element-editor/ai-agent-skills/hooks/useAgentSkills';
import {useAiAgentSkillsStore} from '@/pages/platform/cluster-element-editor/ai-agent-skills/stores/useAiAgentSkillsStore';
import {DataPillPanelSkeleton} from '@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Suspense, lazy} from 'react';
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
    const {evalsPanelOpen, setEvalsPanelOpen} = useAiAgentEvalsStore();
    const {setSkillsPanelOpen, skillsHeaderInfo, skillsPanelOpen} = useAiAgentSkillsStore();

    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);

    const ff_4545 = useFeatureFlagsStore()('ff-4545');
    const ff_4553 = useFeatureFlagsStore()('ff-4553');
    const ff_4554 = useFeatureFlagsStore()('ff-4554');
    const ff_4572 = useFeatureFlagsStore()('ff-4572');

    const {handleNodeDetailsPanelClose, showNodeDetailsPanel, updateWorkflowMutation} = useAiAgentEditor({
        previousComponentDefinitions,
        workflowNodeOutputs,
    });
    const {handleClose: handleEvalsClose} = useAgentEvals();
    const {handleClose: handleSkillsClose} = useAgentSkills({enabled: skillsPanelOpen});

    if (evalsPanelOpen) {
        return (
            <div className={twMerge('flex h-full flex-1 flex-col rounded-lg bg-white', className)}>
                <AiAgentHeader
                    copilotEnabled={ff_4572 && copilotEnabled}
                    onClose={handleEvalsClose}
                    onCopilotClick={onCopilotClick}
                    title="Evals"
                />

                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
                    <AiAgentEvals />
                </div>
            </div>
        );
    }

    if (skillsPanelOpen) {
        return (
            <div className={twMerge('flex h-full flex-1 flex-col rounded-lg bg-white', className)}>
                <AiAgentHeader
                    copilotEnabled={ff_4554 && copilotEnabled}
                    onClose={handleSkillsClose}
                    onCopilotClick={onCopilotClick}
                    subtitle={skillsHeaderInfo.subtitle}
                    title={skillsHeaderInfo.title}
                />

                <div className="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden pl-2 pr-4">
                    <AiAgentSkills />
                </div>
            </div>
        );
    }

    return (
        <div className={twMerge('flex h-full flex-1 flex-col rounded-lg bg-white', className)}>
            <AiAgentHeader
                copilotEnabled={copilotEnabled}
                onClose={onClose}
                onCopilotClick={onCopilotClick}
                onEvalsClick={ff_4553 ? () => setEvalsPanelOpen(true) : undefined}
                onSkillsClick={ff_4545 ? () => setSkillsPanelOpen(true) : undefined}
                onToggleEditor={onToggleEditor}
            />

            <div className="grid min-h-0 flex-1 grid-cols-2 gap-6 overflow-hidden px-4 pt-4">
                <div className="min-h-0 overflow-y-auto">
                    <AiAgentConfigurationPanel />
                </div>

                <div className="relative mb-4 min-h-0">
                    <div className="size-full overflow-hidden">
                        <AiAgentTestingPanel />
                    </div>

                    {showNodeDetailsPanel && previousComponentDefinitions && workflowNodeOutputs && (
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
