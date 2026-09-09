import {AlertTriangleIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import useNodeIssues from '../hooks/useNodeIssues';
import {useWorkflowEditorReadOnly} from '../providers/workflowEditorReadOnlyContext';

interface WorkflowNodeIssueBadgeProps {
    clusterElement?: boolean;
    nodeName: string;
}

const WorkflowNodeIssueBadge = ({clusterElement, nodeName}: WorkflowNodeIssueBadgeProps) => {
    const readOnly = useWorkflowEditorReadOnly();
    const {count, severity, title} = useNodeIssues(nodeName, clusterElement);

    if (readOnly || count === 0) {
        return null;
    }

    return (
        <span
            aria-label={count === 1 ? '1 issue' : `${count} issues`}
            className="absolute -top-2 -right-2 z-10 rounded-full bg-background"
            role="img"
            title={title}
        >
            <AlertTriangleIcon
                className={twMerge(
                    'size-4',
                    severity === 'ERROR' ? 'text-content-destructive' : 'text-content-onwarning'
                )}
            />
        </span>
    );
};

export default WorkflowNodeIssueBadge;
