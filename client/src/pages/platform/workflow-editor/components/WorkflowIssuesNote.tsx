import Button from '@/components/Button/Button';
import {Panel} from '@xyflow/react';
import {AlertTriangleIcon} from 'lucide-react';
import {ReactNode, useCallback, useLayoutEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import useFlowCenterOffset from '../hooks/useFlowCenterOffset';
import useWorkflowIssues from '../hooks/useWorkflowIssues';
import {useWorkflowEditorReadOnly} from '../providers/workflowEditorReadOnlyContext';
import describeWorkflowIssueCounts from '../utils/describeWorkflowIssueCounts';
import openIssuesSidebar from '../utils/openIssuesSidebar';

interface WorkflowIssuesNoteProps {
    fallback: ReactNode;
}

const WorkflowIssuesNote = ({fallback}: WorkflowIssuesNoteProps) => {
    const [noteWidth, setNoteWidth] = useState(0);

    const noteRef = useRef<HTMLDivElement>(null);

    const readOnly = useWorkflowEditorReadOnly();
    const flowCenterOffset = useFlowCenterOffset(noteWidth);
    const issues = useWorkflowIssues();

    const showsNote = !readOnly && issues.length > 0;

    const handleViewClick = useCallback(() => openIssuesSidebar(), []);

    useLayoutEffect(() => {
        const noteElement = noteRef.current;

        if (!showsNote || !noteElement) {
            return;
        }

        const updateNoteWidth = () => setNoteWidth(noteElement.offsetWidth);

        updateNoteWidth();

        const resizeObserver = new ResizeObserver(updateNoteWidth);

        resizeObserver.observe(noteElement);

        return () => resizeObserver.disconnect();
    }, [showsNote]);

    if (readOnly) {
        return null;
    }

    if (issues.length === 0) {
        return fallback;
    }

    const errorCount = issues.filter((issue) => issue.severity === 'ERROR').length;
    const warningCount = issues.length - errorCount;
    const hasErrors = errorCount > 0;

    return (
        <Panel
            position="top-center"
            style={{margin: '8px 0 0', transform: `translateX(calc(-50% + ${flowCenterOffset}px))`}}
        >
            <div
                className={twMerge(
                    'flex items-center gap-2 rounded-md border px-3 py-1.5 shadow-sm',
                    hasErrors
                        ? 'border-stroke-destructive-secondary bg-surface-destructive-secondary'
                        : 'border-stroke-warning-secondary bg-surface-warning-secondary'
                )}
                ref={noteRef}
            >
                <AlertTriangleIcon
                    className={twMerge(
                        'size-4 shrink-0',
                        hasErrors ? 'text-content-destructive' : 'text-content-onwarning'
                    )}
                />

                <span className="text-xs font-medium whitespace-nowrap text-content-neutral-primary">
                    {describeWorkflowIssueCounts(errorCount, warningCount)} in this workflow
                </span>

                <Button
                    className="text-xs font-medium hover:bg-transparent hover:underline"
                    label="View"
                    onClick={handleViewClick}
                    size="xs"
                    variant="ghost"
                />
            </div>
        </Panel>
    );
};

export default WorkflowIssuesNote;
