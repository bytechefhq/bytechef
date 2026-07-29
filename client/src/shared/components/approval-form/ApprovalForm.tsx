import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {renderFormField} from '@/shared/components/form/renderFormField';
import useApprovalForm from '@/shared/hooks/useApprovalForm';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

interface ApprovalFormPropsI {
    id: string | undefined;
    onSubmitted?: (approved: boolean) => void;
    setDocumentTitle?: boolean;
    showHeader?: boolean;

    /**
     * Optional submission transport replacing the default resume-job mutation — used by chat surfaces to resolve
     * through their SSE machinery so the resumed run's output streams back into the conversation.
     */
    submitHandler?: (data: Record<string, unknown>, approved: boolean) => Promise<void>;
}

export default function ApprovalForm({
    id,
    onSubmitted,
    setDocumentTitle = false,
    showHeader = true,
    submitHandler,
}: ApprovalFormPropsI) {
    const [comment, setComment] = useState('');

    const {approved, definition, error, form, handleSubmit, loading, submitError, submitted, submitting, uiDefinition} =
        useApprovalForm(id, {onSubmitted, submitOverride: submitHandler});

    const [searchParams] = useSearchParams();
    const approvedParam = searchParams.get('approved');
    const presetApproved = approvedParam === 'true' ? true : approvedParam === 'false' ? false : null;

    // "comment" is a reserved key in the approval outcome; a user-defined field with that name takes precedence and
    // suppresses the built-in comment box.
    const hasCommentField = !!uiDefinition?.inputs?.some((formInput) => formInput.fieldName === 'comment');

    const withComment = (values: Record<string, unknown>) => {
        const trimmedComment = comment.trim();

        return trimmedComment && !hasCommentField ? {...values, comment: trimmedComment} : values;
    };

    useEffect(() => {
        if (!setDocumentTitle) {
            return;
        }

        const formTitle = uiDefinition?.title;

        document.title = formTitle ? `ByteChef - ${formTitle}` : 'ByteChef';

        return () => {
            document.title = 'ByteChef';
        };
    }, [setDocumentTitle, uiDefinition?.title]);

    if (loading) {
        return (
            <div className="p-6 text-center text-sm text-muted-foreground" role="status">
                Loading form...
            </div>
        );
    }

    if (error || !uiDefinition || !definition) {
        return (
            <div className="p-6 text-center">
                <h2 className="text-lg font-semibold tracking-tight">Form no longer available</h2>

                <p className="mt-2 text-sm text-muted-foreground">
                    This approval form is no longer available. It may have already been submitted, expired, or the link
                    is invalid.
                </p>
            </div>
        );
    }

    if (submitted) {
        return (
            <div className="p-6 text-center" role="status">
                <h2 className="text-lg font-semibold tracking-tight">{approved ? 'Approved' : 'Discarded'}</h2>

                <p className="mt-2 text-sm text-muted-foreground">
                    {approved ? 'Your approval has been submitted.' : 'The request has been discarded.'}
                </p>
            </div>
        );
    }

    // A one-click Approve/Discard link only pre-selects the decision; it must never submit on page load. Email link
    // scanners and messenger preview bots follow links in delivered messages, so an on-load submit would let a
    // crawler silently resolve the approval. Approvals with form fields ignore the shortcut and render the full form.
    if (presetApproved !== null && !uiDefinition.inputs?.length) {
        return (
            <div className="mx-auto max-w-md p-6 text-center">
                <h2 className="text-lg font-semibold tracking-tight">{uiDefinition.title || 'Approval requested'}</h2>

                {uiDefinition.description && (
                    <p className="mt-2 text-sm whitespace-pre-line text-muted-foreground">{uiDefinition.description}</p>
                )}

                <p className="mt-4 text-sm text-muted-foreground">
                    {presetApproved ? 'Confirm to approve this request.' : 'Confirm to discard this request.'}
                </p>

                {!hasCommentField && (
                    <div className="mt-4 space-y-2 text-left">
                        <Label htmlFor="approval-comment">Comment (optional)</Label>

                        <Textarea
                            id="approval-comment"
                            onChange={(event) => setComment(event.target.value)}
                            placeholder="Add a note for the requester — included whether you approve or discard."
                            value={comment}
                        />
                    </div>
                )}

                {submitError && (
                    <p className="mt-4 text-sm text-destructive" role="alert">
                        {submitError}
                    </p>
                )}

                <Button
                    className="mt-6"
                    disabled={submitting}
                    onClick={() => handleSubmit(withComment({}), presetApproved)}
                    type="button"
                    variant={presetApproved ? 'default' : 'outline'}
                >
                    {submitting ? 'Submitting...' : presetApproved ? 'Confirm approval' : 'Confirm discard'}
                </Button>
            </div>
        );
    }

    return (
        <div className="w-full">
            {showHeader && (
                <div className="mb-6">
                    <h2 className="text-lg font-semibold tracking-tight">{uiDefinition.title}</h2>

                    {uiDefinition.description && (
                        <p className="mt-2 text-sm whitespace-pre-line text-muted-foreground">
                            {uiDefinition.description}
                        </p>
                    )}
                </div>
            )}

            <Form {...form}>
                <form
                    className="space-y-6"
                    onSubmit={(event) => {
                        event.preventDefault();
                    }}
                >
                    {uiDefinition.inputs?.length ? (
                        uiDefinition.inputs.map((formInput, idx) => {
                            const name = formInput.fieldName || `field_${idx}`;

                            return <div key={name}>{renderFormField(form, formInput, name)}</div>;
                        })
                    ) : (
                        <span className="text-sm text-muted-foreground">No inputs defined.</span>
                    )}

                    {!hasCommentField && (
                        <div className="space-y-2">
                            <Label htmlFor="approval-comment">Comment (optional)</Label>

                            <Textarea
                                id="approval-comment"
                                onChange={(event) => setComment(event.target.value)}
                                placeholder="Add a note for the requester — included whether you approve or discard."
                                value={comment}
                            />
                        </div>
                    )}

                    {submitError && (
                        <p className="text-sm text-destructive" role="alert">
                            {submitError}
                        </p>
                    )}

                    <div className="mt-4 flex gap-3">
                        <Button
                            disabled={submitting}
                            onClick={form.handleSubmit((values) => handleSubmit(withComment(values), true))}
                            type="button"
                        >
                            {submitting ? 'Submitting...' : 'Approve'}
                        </Button>

                        <Button
                            disabled={submitting}
                            onClick={() => handleSubmit(withComment(form.getValues()), false)}
                            type="button"
                            variant="outline"
                        >
                            {submitting ? 'Submitting...' : 'Discard'}
                        </Button>
                    </div>
                </form>
            </Form>
        </div>
    );
}
