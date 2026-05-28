import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowTestChatRuntimeProvider} from '@/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {XIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useShallow} from 'zustand/react/shallow';

const WorkflowTestChatPanel = () => {
    const {generateConversationId, setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} = useWorkflowTestChatStore(
        useShallow((state) => ({
            generateConversationId: state.generateConversationId,
            setWorkflowTestChatPanelOpen: state.setWorkflowTestChatPanelOpen,
            workflowTestChatPanelOpen: state.workflowTestChatPanelOpen,
        }))
    );

    const handlePanelClose = () => {
        setWorkflowTestChatPanelOpen(false);
    };

    useEffect(() => {
        generateConversationId();
    }, [generateConversationId, workflowTestChatPanelOpen]);

    if (!workflowTestChatPanelOpen) {
        return <></>;
    }

    return (
        <div className="absolute inset-y-4 top-2 right-[69px] bottom-6 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background">
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-surface-main">
                <header className="flex items-center p-4 text-lg font-medium">
                    <span>Playground</span>

                    <button
                        aria-label="Close the node details dialog"
                        className="ml-auto pr-0"
                        onClick={handlePanelClose}
                    >
                        <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                    </button>
                </header>

                <div className="absolute inset-x-0 top-16 bottom-0">
                    <WorkflowTestChatRuntimeProvider>
                        <Thread />
                    </WorkflowTestChatRuntimeProvider>
                </div>
            </div>
        </div>
    );
};

export default WorkflowTestChatPanel;
