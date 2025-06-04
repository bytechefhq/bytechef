import React, {useEffect} from 'react';
import Logo from './assets/logo.svg';
import {SquareArrowOutUpRightIcon, XIcon} from 'lucide-react';
import {DialogStepType} from '.';
import {twMerge} from 'tailwind-merge';

interface DialogProps {
    closeDialog: () => void;
    dialogStep: DialogStepType;
    form?: any;
    handleContinue: () => void;
    handleSubmit: () => void;
    integration: any;
    isOAuth2?: boolean;
    isOpen: boolean;
    properties?: any[];
    registerFormSubmit?: (submitFn: (data: any) => void) => void;
}

const ConnectDialog = ({
    closeDialog,
    dialogStep,
    form,
    handleContinue,
    handleSubmit,
    integration,
    isOAuth2 = false,
    isOpen,
    properties,
    registerFormSubmit,
}: DialogProps) => {
    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                closeDialog();
            }
        };

        if (isOpen) {
            window.addEventListener('keydown', handleKeyDown);
        }

        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isOpen, closeDialog]);

    if (!isOpen) {
        return null;
    }

    return (
        <div
            data-testid="dialog-overlay"
            className="fixed inset-0 z-50 bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
            onClick={closeDialog}
        >
            <div
                className="fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-8 border bg-background p-6 shadow-lg duration-200 sm:rounded-lg"
                onClick={(event) => event.stopPropagation()}
            >
                <DialogHeader closeDialog={closeDialog} integration={integration} />

                <DialogContent
                    closeDialog={closeDialog}
                    dialogStep={dialogStep}
                    form={form}
                    integration={integration}
                    properties={properties}
                    registerFormSubmit={registerFormSubmit}
                />

                {integration && (
                    <DialogFooter
                        closeDialog={closeDialog}
                        dialogStep={dialogStep}
                        handleContinue={handleContinue}
                        handleSubmit={dialogStep === 'form' ? handleSubmit : undefined}
                        isOAuth2={isOAuth2}
                    />
                )}

                <DialogPoweredBy />
            </div>
        </div>
    );
};

const DialogHeader = ({closeDialog, integration}: {closeDialog: () => void; integration: any}) => (
    <header
        className={twMerge('grid grid-cols-[1fr_auto_1fr] items-center', !integration?.icon && 'flex justify-between')}
    >
        {integration && integration.icon && (
            <div className="flex [&_svg]:size-8" dangerouslySetInnerHTML={{__html: integration.icon}} />
        )}

        <h2 className="whitespace-nowrap text-lg font-semibold">Create Connection</h2>

        <button className="flex justify-end rounded-md p-2 transition-all hover:bg-slate-100" onClick={closeDialog}>
            <XIcon className="size-4" />

            <span className="sr-only">Close</span>
        </button>
    </header>
);

interface DialogContentProps {
    closeDialog: () => void;
    dialogStep: DialogStepType;
    form: any;
    integration: any;
    properties?: any[];
    registerFormSubmit?: (submitFn: (data: any) => void) => void;
}

const DialogContent = ({
    closeDialog,
    dialogStep = 'initial',
    form,
    integration,
    properties,
    registerFormSubmit,
}: DialogContentProps) => {
    // Register the form's submit handler when the component mounts
    useEffect(() => {
        if (registerFormSubmit) {
            registerFormSubmit(form.handleSubmit);
        }
    }, [registerFormSubmit, form.handleSubmit]);

    if (!integration) {
        return (
            <main className="text-center">
                <h2 className="text-lg font-semibold">Unable to Load Integration</h2>

                <p className="mt-2 text-muted-foreground">
                    We couldn't load the integration data. Please try again later.
                </p>

                <button
                    onClick={closeDialog}
                    className="mt-4 inline-flex h-10 items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                >
                    Close
                </button>
            </main>
        );
    }

    return (
        <main className="flex flex-col gap-4 text-center text-sm sm:text-left">
            {dialogStep === 'initial' && <p className="text-muted-foreground">{integration.description}</p>}

            {dialogStep === 'form' && (
                <form
                    id="form"
                    onSubmit={form.handleSubmit((data: any) => console.log(data))}
                    className="flex flex-col gap-4 text-sm"
                >
                    {properties?.map((property) => {
                        const field = form.register(property.name, {
                            required: property.required,
                            value: property.defaultValue || '',
                            validate: property.validate,
                        });

                        const error = form.formState.errors[property.name];

                        return (
                            <DialogInputField
                                key={property.name}
                                label={property.label}
                                name={property.name}
                                options={property.options}
                                placeholder={property.placeholder}
                                required={property.required}
                                field={field}
                                error={error}
                            />
                        );
                    })}
                </form>
            )}
        </main>
    );
};

interface DialogFooterProps {
    closeDialog: () => void;
    dialogStep: DialogStepType;
    handleContinue?: () => void;
    handleSubmit?: () => void;
    isOAuth2?: boolean;
}

const DialogFooter = ({
    closeDialog,
    handleContinue,
    handleSubmit,
    dialogStep = 'initial',
    isOAuth2 = false,
}: DialogFooterProps) => (
    <footer className="flex items-center justify-end gap-2">
        <button
            type="button"
            className="focus-visible:ring-bytechef inline-flex h-10 items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
            onClick={closeDialog}
        >
            Cancel
        </button>

        <button
            autoFocus
            onClick={dialogStep === 'initial' ? handleContinue : handleSubmit}
            className="bg-bytechef hover:bg-bytechef/90 focus-visible:ring-bytechef inline-flex h-10 items-center justify-center rounded-md px-4 py-2 text-sm font-medium text-primary-foreground ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
            type={dialogStep === 'form' ? 'submit' : 'button'}
            form="form"
        >
            {dialogStep === 'initial' && isOAuth2 && (
                <span className="flex items-center gap-2">
                    Authorize
                    <SquareArrowOutUpRightIcon className="size-4" />
                </span>
            )}

            {dialogStep === 'initial' && !isOAuth2 && 'Continue'}

            {dialogStep !== 'initial' && 'Connect'}
        </button>
    </footer>
);

const DialogPoweredBy = () => (
    <div className="absolute -bottom-8 right-[50%] flex translate-x-[50%] items-center justify-center">
        <img src={Logo} alt="ByteChef Logo" className="mr-2 size-4" />

        <span className="text-sm text-white">
            Powered by
            <a
                className="pl-1 font-semibold hover:underline"
                href="https://bytechef.io"
                target="_blank"
                rel="noopener noreferrer"
            >
                ByteChef
            </a>
        </span>
    </div>
);

interface DialogInputFieldsetProps {
    label: string;
    name: string;
    placeholder?: string;
    options?: string[];
    required?: boolean;
    field?: any;
    error?: any;
}

const DialogInputField = ({label, name, options, placeholder, required, field, error}: DialogInputFieldsetProps) => (
    <fieldset className="space-y-2">
        <label htmlFor={name} className="text-sm font-medium">
            {label}

            {required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
        </label>

        {options ? (
            <select
                id={name}
                className="focus-visible:ring-bytechef flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                {...field}
            >
                <option value="">Select {label}</option>

                {options.map((option) => (
                    <option key={option} value={option}>
                        {option}
                    </option>
                ))}
            </select>
        ) : (
            <input
                id={name}
                className="focus-visible:ring-bytechef flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                placeholder={placeholder}
                {...field}
            />
        )}

        {error && <p className="mt-1 text-xs text-red-500">{error.message}</p>}
    </fieldset>
);

export default ConnectDialog;
