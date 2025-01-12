import {WorkflowTestChatRuntimeProvider} from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatRuntimeProvider';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {Thread} from '@assistant-ui/react';
import {Cross2Icon} from '@radix-ui/react-icons';

const WorkflowTestChatPanel = () => {
    const {setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} = useWorkflowTestChatStore();

    const handlePanelClose = () => {
        setWorkflowTestChatPanelOpen(false);
    };

    if (!workflowTestChatPanelOpen) {
        return <></>;
    }

    return (
        <div className="absolute inset-y-4 right-4 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-xl border border-border/50 bg-white shadow-lg">
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
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

                <div className="absolute inset-x-0 bottom-0 top-16 text-sm">
                    <WorkflowTestChatRuntimeProvider>
                        <Thread />
                    </WorkflowTestChatRuntimeProvider>
                </div>
            </div>
        </div>
    );
};

export default WorkflowTestChatPanel;
