import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowTestChatRuntimeProvider} from '@/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useEffect} from 'react';

const WorkflowTestChatPanel = () => {
    const {generateConversationId, setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} =
        useWorkflowTestChatStore();

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
        <div className="absolute inset-y-4 bottom-6 right-[69px] top-2 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-l border border-stroke-neutral-secondary bg-background">
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-surface-main">
                <header className="flex items-center p-4 text-lg font-medium">
                    <span>Playground</span>

                    <button
                        aria-label="Close the node details dialog"
                        className="ml-auto pr-0"
                        onClick={handlePanelClose}
                    >
                        <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                    </button>
                </header>

                <div className="absolute inset-x-0 bottom-0 top-16">
                    <WorkflowTestChatRuntimeProvider>
                        <Thread />
                    </WorkflowTestChatRuntimeProvider>
                </div>
            </div>
        </div>
    );
};

export default WorkflowTestChatPanel;
