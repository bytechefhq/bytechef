import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {renderFormField} from '@/shared/components/form/renderFormField';
import {PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import useApprovalForm from '@/shared/hooks/useApprovalForm';
import {useEffect, useRef} from 'react';
import {useSearchParams} from 'react-router-dom';

interface ApprovalFormPropsI {
    id: string | undefined;
    onSubmitted?: (approved: boolean) => void;
    showHeader?: boolean;
}

export default function ApprovalForm({id, onSubmitted, showHeader = true}: ApprovalFormPropsI) {
    const {approved, definition, error, form, handleSubmit, loading, submitError, submitted, submitting, uiDefinition} =
        useApprovalForm(id, {onSubmitted});

    const [searchParams] = useSearchParams();
    const approvedParam = searchParams.get('approved');
    const autoApproved = approvedParam === 'true' ? true : approvedParam === 'false' ? false : null;
    const autoSubmittedRef = useRef(false);

    useEffect(() => {
        if (autoApproved !== null && !autoSubmittedRef.current) {
            autoSubmittedRef.current = true;

            void handleSubmit({}, autoApproved);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [autoApproved]);

    if (submitError) {
        return (
            <div className="p-6">
                <span className="text-sm text-destructive">{submitError}</span>
            </div>
        );
    }

    if (autoApproved !== null) {
        const resolvedApproved = approved ?? autoApproved;

        return (
            <div className="p-6 text-center">
                <h2 className="text-lg font-semibold tracking-tight">{resolvedApproved ? 'Approved' : 'Discarded'}</h2>

                <p className="mt-2 text-sm text-muted-foreground">
                    {resolvedApproved ? 'Your approval has been submitted.' : 'The request has been discarded.'}
                </p>
            </div>
        );
    }

    if (loading) {
        return <div className="p-6 text-center text-sm text-muted-foreground">Loading form...</div>;
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
            <div className="p-6 text-center">
                <h2 className="text-lg font-semibold tracking-tight">{approved ? 'Approved' : 'Discarded'}</h2>

                <p className="mt-2 text-sm text-muted-foreground">
                    {approved ? 'Your approval has been submitted.' : 'The request has been discarded.'}
                </p>
            </div>
        );
    }

    return (
        <div className="w-full">
            {uiDefinition.environmentId != null && uiDefinition.environmentId !== PRODUCTION_ENVIRONMENT && (
                <div className="absolute space-x-1 p-3 uppercase">
                    <span>Environment:</span>

                    <span className="font-semibold">{uiDefinition.environmentName}</span>
                </div>
            )}

            {showHeader && (
                <div className="mb-6">
                    <h2 className="text-lg font-semibold tracking-tight">{uiDefinition.title}</h2>

                    {uiDefinition.description && (
                        <p className="mt-2 whitespace-pre-line text-sm text-muted-foreground">
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

                    <div className="mt-4 flex gap-3">
                        <Button
                            disabled={submitting}
                            onClick={form.handleSubmit((values) => handleSubmit(values, true))}
                            type="button"
                        >
                            {submitting ? 'Submitting...' : 'Approve'}
                        </Button>

                        <Button
                            disabled={submitting}
                            onClick={form.handleSubmit((values) => handleSubmit(values, false))}
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
