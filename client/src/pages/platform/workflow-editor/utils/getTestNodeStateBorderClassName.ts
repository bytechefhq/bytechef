import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

export default function getTestNodeStateBorderClassName(testNodeState?: WorkflowTestNodeStateI): string | undefined {
    switch (testNodeState?.status) {
        case 'RUNNING':
            return 'border-blue-500 hover:border-blue-500';
        case 'COMPLETED':
            return 'border-green-500 hover:border-green-500';
        case 'FAILED':
            return 'border-red-500 hover:border-red-500';
        default:
            return undefined;
    }
}
