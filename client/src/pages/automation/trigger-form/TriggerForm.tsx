import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {renderFormField} from '@/shared/components/form/renderFormField';
import {PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import React from 'react';

import useTriggerForm from './hooks/useTriggerForm';

export default function TriggerForm() {
    const {
        definition,
        environmentId,
        environmentName,
        error,
        form,
        handleSubmit,
        loading,
        submitError,
        submitted,
        submitting,
        uiDefinition,
    } = useTriggerForm();

    if (loading) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    Loading form…
                </div>
            </div>
        );
    }

    if (error || submitError) {
        return (
            <div className="h-full overflow-auto">
                <div className="p-6">
                    <span className="text-sm text-destructive">
                        {error ? (error.message ? error.message : 'Failed to load trigger form') : submitError}
                    </span>
                </div>
            </div>
        );
    }

    if (submitted) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center">
                    <h1 className="text-2xl font-semibold tracking-tight">Thank you!</h1>

                    <p className="mt-2 text-sm text-muted-foreground">Your response has been submitted.</p>
                </div>
            </div>
        );
    }

    if (!uiDefinition || !definition) {
        return (
            <div className="h-full overflow-auto">
                <span className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    No definition found.
                </span>
            </div>
        );
    }

    return (
        <div className="h-full overflow-auto">
            {+(environmentId ?? PRODUCTION_ENVIRONMENT) !== PRODUCTION_ENVIRONMENT && (
                <div className="absolute space-x-1 p-3 uppercase">
                    <span>Environment:</span>

                    <span className="font-semibold">{environmentName}</span>
                </div>
            )}

            <div className="mx-auto mt-6 w-full max-w-2xl p-6">
                {uiDefinition.customFormStyling && <style>{uiDefinition.customFormStyling}</style>}

                <div className="mb-6">
                    <h1 className="text-2xl font-semibold tracking-tight">{uiDefinition.title}</h1>

                    {uiDefinition.subtitle && (
                        <p className="mt-2 whitespace-pre-line text-sm text-muted-foreground">
                            {uiDefinition.subtitle}
                        </p>
                    )}
                </div>

                <Form {...form}>
                    <form className="space-y-6" onSubmit={form.handleSubmit(handleSubmit)}>
                        {uiDefinition.inputs?.length ? (
                            uiDefinition.inputs.map((formInput, idx) => {
                                const name = formInput.fieldName || `field_${idx}`;

                                return <div key={name}>{renderFormField(form, formInput, name)}</div>;
                            })
                        ) : (
                            <span className="text-sm text-muted-foreground">No inputs defined.</span>
                        )}

                        <Button className="mt-2" disabled={submitting} type="submit">
                            {submitting ? 'Submitting...' : uiDefinition.buttonLabel}
                        </Button>
                    </form>
                </Form>

                {uiDefinition.appendAttribution && (
                    <div className="mt-8 space-x-1 border-t pt-4 text-center text-xs text-muted-foreground">
                        <span>Powered by</span>

                        <a href="https://www.bytechef.io" rel="noopener noreferrer" target="_blank">
                            ByteChef
                        </a>
                    </div>
                )}
            </div>
        </div>
    );
}
