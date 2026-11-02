import {FC, useState} from 'react';
import {ApprovalFormField} from '@/components/ApprovalFormField';
import {useApprovalResolution} from '@/components/approvalResolutionContext';
import {PendingApprovalI, useChatStore} from '@/stores/useChatStore';
import {
    getApprovalFormDefaultValues,
    hasCommentField,
    toApprovalFormData,
    validateApprovalForm,
} from '@/utils/approval-form';
import type {ApprovalFormValuesType} from '@/utils/approval-form';
import {ApprovalResumeError, isAcceptedResumeResponse, toApprovalResumeError} from '@/utils/approval-utils';
import {Button} from './ui/button';

export const ApprovalCard: FC = () => {
    const pendingApproval = useChatStore((state) => state.pendingApproval);

    if (!pendingApproval) {
        return null;
    }

    return <ApprovalCardContent key={pendingApproval.resumeUrl} pendingApproval={pendingApproval} />;
};

const ApprovalCardContent: FC<{pendingApproval: PendingApprovalI}> = ({pendingApproval}) => {
    const setMessage = useChatStore((state) => state.setMessage);
    const setPendingApproval = useChatStore((state) => state.setPendingApproval);

    const approvalResolution = useApprovalResolution();

    const inputs = pendingApproval.inputs ?? [];

    const [comment, setComment] = useState('');
    const [editedFieldNames, setEditedFieldNames] = useState<ReadonlySet<string>>(() => new Set());
    const [error, setError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [submitting, setSubmitting] = useState(false);
    const [values, setValues] = useState<ApprovalFormValuesType>(() => getApprovalFormDefaultValues(inputs));

    const title = pendingApproval.formTitle || 'Approval requested';
    const showCommentField = !hasCommentField(inputs);

    const resolve = async (approved: boolean) => {
        if (approved) {
            const validationErrors = validateApprovalForm(inputs, values);

            setFieldErrors(validationErrors);

            if (Object.keys(validationErrors).length > 0) {
                return;
            }
        }

        setSubmitting(true);
        setError(null);

        const body: Record<string, unknown> = {...toApprovalFormData(inputs, values, editedFieldNames), approved};
        const trimmedComment = comment.trim();

        if (trimmedComment && showCommentField) {
            body.comment = trimmedComment;
        }

        try {
            if (approvalResolution) {
                await approvalResolution.resolveApproval(pendingApproval, body);

                setComment('');

                return;
            }

            const response = await fetch(pendingApproval.resumeUrl, {
                body: JSON.stringify(body),
                headers: {'Content-Type': 'application/json'},
                method: 'POST',
            });

            if (!isAcceptedResumeResponse(response)) {
                throw await toApprovalResumeError(response);
            }

            setPendingApproval(null);
            setComment('');
            setMessage({
                content: `**${title}** — ${approved ? 'approved' : 'discarded'}.`,
                role: 'assistant',
            });
        } catch (resolveError) {
            console.error('Failed to resolve approval:', resolveError);

            const userMessage = resolveError instanceof ApprovalResumeError ? resolveError.userMessage : null;

            setError(userMessage ?? 'Failed to submit your decision. Please try again.');
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

            {inputs.map((input) => {
                const fieldName = input.fieldName!;

                return (
                    <ApprovalFormField
                        disabled={submitting}
                        error={fieldErrors[fieldName]}
                        input={input}
                        key={fieldName}
                        onChange={(value) => {
                            setValues((currentValues) => ({...currentValues, [fieldName]: value}));
                            setEditedFieldNames((currentFieldNames) => new Set(currentFieldNames).add(fieldName));
                        }}
                        value={values[fieldName]}
                    />
                );
            })}

            {showCommentField && (
                <textarea
                    aria-label="Comment (optional)"
                    className="min-h-16 w-full resize-y rounded-md border border-border bg-background p-2 text-sm outline-none"
                    disabled={submitting}
                    onChange={(changeEvent) => setComment(changeEvent.target.value)}
                    placeholder="Add a note for the requester — included whether you approve or discard."
                    value={comment}
                />
            )}

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
