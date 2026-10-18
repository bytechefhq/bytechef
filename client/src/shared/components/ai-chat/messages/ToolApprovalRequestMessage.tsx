import Button from '@/components/Button/Button';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useApprovalResolution} from '@/shared/components/ai-chat/approvalResolutionContext';
import {DataMessagePartProps} from '@assistant-ui/react';
import {WrenchIcon} from 'lucide-react';
import {useState} from 'react';

export interface ToolApprovalRequestDataI {
    approvalId: number;
    arguments: Record<string, unknown>;
    awaitingApproval: boolean;
    componentName?: string | null;
    expiresAt?: string | null;
    kind: 'tool-approval-request';
    toolName: string;
}

/**
 * Extra fields stitched onto {@link ToolApprovalRequestDataI} when a chat reload discovers the approval has
 * already been decided (see {@code useSwitchChat}'s approvals-list mapping). Not part of the tool result payload
 * itself; the gated tool call only ever reports the pending request, never its eventual outcome.
 */
type ToolApprovalRequestMessageDataType = ToolApprovalRequestDataI & {
    executionError?: string;
    resolvedBy?: string;
    resolvedStatus?: string;
};

const ARGUMENT_COLLAPSE_LENGTH = 200;

const formatArgumentValue = (value: unknown): string => (typeof value === 'string' ? value : JSON.stringify(value));

/**
 * Maps a settled approval to its status line. {@code resolvedStatus} (from the approvals-list reload lookup) wins
 * when present; it can be any terminal status, including ones this card's own buttons never produce (EXPIRED,
 * SUPERSEDED). Absent that, {@code localStatus}; the row's actual status as returned by this session's own
 * Approve/Reject click; covers the outcomes this card itself can cause, which is not only APPROVED/REJECTED: an
 * approved call can still come back FAILED if the tool itself throws, and the card must say so rather than claim
 * "Approved - the tool ran." for a call that never actually ran to completion.
 */
const resolvedStatusMessage = (
    status: string | undefined,
    executionError: string | undefined | null
): string | null => {
    switch (status) {
        case 'APPROVED':
            return 'Approved - the tool ran.';
        case 'EXPIRED':
            return 'Expired.';
        case 'FAILED':
            return `Failed: ${executionError ?? 'unknown error'}`;
        case 'REJECTED':
            return 'Rejected.';
        case 'SUPERSEDED':
            return 'Superseded by a newer message.';
        default:
            return null;
    }
};

/** One argument row, collapsing values past {@link ARGUMENT_COLLAPSE_LENGTH} behind a "Show more" toggle. */
const ToolApprovalArgumentRow = ({label, value}: {label: string; value: string}) => {
    const [expanded, setExpanded] = useState(false);

    const isLong = value.length > ARGUMENT_COLLAPSE_LENGTH;
    const displayValue = isLong && !expanded ? `${value.slice(0, ARGUMENT_COLLAPSE_LENGTH)}…` : value;

    return (
        <div className="grid grid-cols-[minmax(0,120px)_1fr] gap-2 text-sm">
            <dt className="truncate font-medium text-muted-foreground">{label}</dt>

            <dd className="min-w-0 break-words whitespace-pre-wrap">
                {displayValue}

                {isLong && (
                    <button
                        className="ml-1 text-xs text-muted-foreground underline"
                        onClick={() => setExpanded((current) => !current)}
                        type="button"
                    >
                        {expanded ? 'Show less' : 'Show more'}
                    </button>
                )}
            </dd>
        </div>
    );
};

/** This session's own Approve/Reject outcome, as actually returned by the resolve mutation. */
interface LocalResolutionI {
    executionError?: string | null;
    status: string;
}

/**
 * Renders a gated AI Hub tool call awaiting a person's approval as an inline card in the chat conversation. The
 * gated tool never executes on its own; this is the ONLY UI that can move it forward, via
 * {@link ApprovalResolutionContextI.resolveToolApproval}, which POSTs the decision through the
 * {@code resolveAiHubToolApproval} GraphQL mutation.
 *
 * <p>This card gates a single AI Hub tool call and is resolved through its own mutation, so it never falls back to
 * {@code resolveApproval}; without a tool-approval resolver it renders inert buttons.
 *
 * <p>Once {@code resolvedStatus} is known (a chat reload discovered the approval already settled) or this card's
 * own buttons resolved it in this session, the interactive controls are replaced by a status line; the approval
 * can only ever be decided once.
 */
const ToolApprovalRequestMessage = ({data}: DataMessagePartProps<ToolApprovalRequestMessageDataType>) => {
    const [comment, setComment] = useState('');
    const [localResolution, setLocalResolution] = useState<LocalResolutionI | null>(null);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    const approvalResolution = useApprovalResolution();

    const {approvalId, arguments: toolArguments, componentName, toolName} = data;

    const statusMessage = resolvedStatusMessage(
        data.resolvedStatus ?? localResolution?.status,
        data.executionError ?? localResolution?.executionError
    );

    const resolve = async (approved: boolean) => {
        setSubmitting(true);
        setSubmitError(null);

        const trimmedComment = comment.trim();

        try {
            const result = await approvalResolution?.resolveToolApproval?.(
                approvalId,
                approved,
                trimmedComment || undefined
            );

            setLocalResolution(result ?? {status: approved ? 'APPROVED' : 'REJECTED'});
        } catch (error) {
            setSubmitError(
                error instanceof Error ? error.message : 'Failed to submit your decision. Please try again.'
            );
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="mt-2 flex flex-col gap-3 rounded-md border border-border bg-muted/30 p-3">
            <div className="flex items-center gap-2 text-xs font-medium tracking-wide text-muted-foreground uppercase">
                <WrenchIcon className="size-3.5" />
                Tool approval required
            </div>

            <div className="text-sm font-semibold">{componentName ? `${componentName}/${toolName}` : toolName}</div>

            {Object.keys(toolArguments).length > 0 && (
                <dl className="flex flex-col gap-1">
                    {Object.entries(toolArguments).map(([key, value]) => (
                        <ToolApprovalArgumentRow key={key} label={key} value={formatArgumentValue(value)} />
                    ))}
                </dl>
            )}

            {statusMessage !== null ? (
                <div className="text-sm text-muted-foreground" role="status">
                    {statusMessage}

                    {data.resolvedBy && ` by ${data.resolvedBy}`}
                </div>
            ) : (
                <>
                    <div className="space-y-2">
                        <Label htmlFor={`tool-approval-comment-${approvalId}`}>Comment (optional)</Label>

                        <Textarea
                            disabled={submitting}
                            id={`tool-approval-comment-${approvalId}`}
                            onChange={(event) => setComment(event.target.value)}
                            placeholder="Add a note for the requester; included whether you approve or reject."
                            value={comment}
                        />
                    </div>

                    {submitError && (
                        <div className="text-sm text-destructive" role="alert">
                            {submitError}
                        </div>
                    )}

                    <div className="flex gap-3">
                        <Button disabled={submitting} onClick={() => resolve(true)} size="sm" type="button">
                            {submitting ? 'Submitting…' : 'Approve'}
                        </Button>

                        <Button
                            disabled={submitting}
                            onClick={() => resolve(false)}
                            size="sm"
                            type="button"
                            variant="outline"
                        >
                            {submitting ? 'Submitting…' : 'Reject'}
                        </Button>
                    </div>
                </>
            )}
        </div>
    );
};

export default ToolApprovalRequestMessage;
