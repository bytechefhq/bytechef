import {WorkflowNodeChangeKind} from '@/shared/middleware/graphql';

export default function getChangeHighlightBorderClassName(kind?: WorkflowNodeChangeKind): string | undefined {
    switch (kind) {
        case WorkflowNodeChangeKind.Added:
            return 'border-green-500 hover:border-green-500';
        case WorkflowNodeChangeKind.Modified:
            return 'border-amber-500 hover:border-amber-500';
        case WorkflowNodeChangeKind.Moved:
            return 'border-blue-500 hover:border-blue-500';
        case WorkflowNodeChangeKind.Removed:
            return 'border-red-500 hover:border-red-500';
        default:
            return undefined;
    }
}
