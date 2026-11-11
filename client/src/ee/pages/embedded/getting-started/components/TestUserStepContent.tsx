import WorkflowExecutionContentClipboardButton from '@/shared/components/workflow-executions/WorkflowExecutionContentClipboardButton';

interface TestUserStepContentProps {
    externalUserId: string;
}

const TestUserStepContent = ({externalUserId}: TestUserStepContentProps) => (
    <div className="flex items-center gap-2 text-xs text-content-neutral-secondary">
        <span>externalUserId</span>

        <code className="rounded bg-surface-neutral-secondary px-1.5 py-0.5 text-content-neutral-primary">
            {externalUserId}
        </code>

        <WorkflowExecutionContentClipboardButton value={externalUserId} />
    </div>
);

export default TestUserStepContent;
