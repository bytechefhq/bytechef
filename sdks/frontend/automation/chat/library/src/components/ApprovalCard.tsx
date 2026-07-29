import {FC, useState} from 'react';
import {useApprovalResolution} from '@/components/approvalResolutionContext';
import {useChatStore} from '@/stores/useChatStore';
import {Button} from './ui/button';

/**
 * Inline resolution card for a field-less approval request, rendered pinned above the composer while the approval is
 * pending. Approve/Discard POST {approved, comment?} directly to the tokenized job-resume endpoint — the chat input
 * itself never resolves an approval. Approvals that carry form fields are not routed here; they render as a markdown
 * link to the hosted approval form (see AutomationChatProvider.handleApprovalRequest).
 */
export const ApprovalCard: FC = () => {
    const pendingApproval = useChatStore((state) => state.pendingApproval);
    const setMessage = useChatStore((state) => state.setMessage);
    const setPendingApproval = useChatStore((state) => state.setPendingApproval);

    const approvalResolution = useApprovalResolution();

    const [comment, setComment] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    if (!pendingApproval) {
        return null;
    }

    const title = pendingApproval.formTitle || 'Approval requested';

    const resolve = async (approved: boolean) => {
        setSubmitting(true);
        setError(null);

        const body: Record<string, unknown> = {approved};
        const trimmedComment = comment.trim();

        if (trimmedComment) {
            body.comment = trimmedComment;
        }

        try {
            if (approvalResolution) {
                // Streams the resumed run's output back into the conversation; the provider clears the pending
                // approval and appends the resolved marker itself.
                await approvalResolution.resolveApproval(pendingApproval, body);

                setComment('');

                return;
            }

            const response = await fetch(pendingApproval.resumeUrl, {
                body: JSON.stringify(body),
                headers: {'Content-Type': 'application/json'},
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Resume request failed with status ${response.status}`);
            }

            setPendingApproval(null);
            setComment('');
            setMessage({
                content: `**${title}** — ${approved ? 'approved' : 'discarded'}.`,
                role: 'assistant',
            });
        } catch (resolveError) {
            console.error('Failed to resolve approval:', resolveError);

            setError('Failed to submit your decision. Please try again.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="bytechef-approval-card mx-auto flex w-full max-w-(--thread-max-width) flex-col gap-3 rounded-xl border border-border bg-muted/30 p-3 text-sm">
            <div className="font-medium">{title}</div>

            {pendingApproval.formDescription && (
                <div className="whitespace-pre-line text-muted-foreground">{pendingApproval.formDescription}</div>
            )}

            {pendingApproval.expiresAt && (
                <div className="text-xs text-muted-foreground">
                    Expires {new Date(pendingApproval.expiresAt).toLocaleString()}
                </div>
            )}

            <textarea
                aria-label="Comment (optional)"
                className="min-h-16 w-full resize-y rounded-md border border-border bg-background p-2 text-sm outline-none"
                disabled={submitting}
                onChange={(changeEvent) => setComment(changeEvent.target.value)}
                placeholder="Add a note for the requester — included whether you approve or discard."
                value={comment}
            />

            {error && (
                <div className="text-destructive" role="alert">
                    {error}
                </div>
            )}

            <div className="flex gap-2">
                <Button disabled={submitting} onClick={() => resolve(true)} size="sm" type="button">
                    {submitting ? 'Submitting…' : 'Approve'}
                </Button>

                <Button disabled={submitting} onClick={() => resolve(false)} size="sm" type="button" variant="outline">
                    {submitting ? 'Submitting…' : 'Discard'}
                </Button>
            </div>
        </div>
    );
};
