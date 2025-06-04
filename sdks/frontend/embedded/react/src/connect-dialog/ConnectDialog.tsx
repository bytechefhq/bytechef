import React, {useEffect} from 'react';
import Logo from './assets/logo.svg';
import {DialogStepType} from '.';

// Inline SVG icons to replace lucide-react
const XIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <path d="M18 6 6 18" />
    <path d="m6 6 12 12" />
  </svg>
);

const SquareArrowOutUpRightIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <path d="M14 10V6a2 2 0 0 0-2-2v0a2 2 0 0 0-2 2v4" />
    <path d="M16 18a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
    <path d="m21 3-9 9" />
    <path d="M15 3h6v6" />
  </svg>
);

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
            style={{
                position: 'fixed',
                inset: 0,
                zIndex: 50,
                backgroundColor: 'rgba(0, 0, 0, 0.5)',
            }}
            onClick={closeDialog}
        >
            <div
                style={{
                    position: 'fixed',
                    left: '50%',
                    top: '50%',
                    zIndex: 50,
                    display: 'grid',
                    width: '100%',
                    maxWidth: '32rem', // max-w-lg
                    transform: 'translate(-50%, -50%)',
                    gap: '2rem', // gap-8
                    border: '1px solid #e5e7eb', // border
                    backgroundColor: '#fff', // bg-background
                    padding: '1.5rem', // p-6
                    boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)', // shadow-lg
                    transitionDuration: '200ms', // duration-200
                    borderRadius: '0.5rem', // sm:rounded-lg
                }}
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

const DialogHeader = ({closeDialog, integration}: {closeDialog: () => void; integration: any}) => {
    // For handling hover state on close button
    const [isCloseHovered, setIsCloseHovered] = React.useState(false);

    return (
        <header
            style={{
                display: 'flex', justifyContent: 'space-between'
            }}
        >
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '1rem'
                }}
            >
                {integration && integration.icon && (
                    <div
                        style={{ display: 'flex' }}
                        dangerouslySetInnerHTML={{__html: integration.icon}}
                    />
                )}

                <h2 style={{
                    whiteSpace: 'nowrap',
                    fontSize: '1.125rem', // text-lg
                    fontWeight: 600 // font-semibold
                }}>
                    Create Connection
                </h2>
            </div>

            <div style={{
                display: 'flex',
                justifyContent: 'flex-end',
            }}>
                <button
                    style={{
                        borderRadius: '0.375rem', // rounded-md
                        padding: '0.5rem', // p-2
                        transition: 'all 0.2s',
                        backgroundColor: isCloseHovered ? '#f1f5f9' : 'transparent'
                    }}
                    onClick={closeDialog}
                    onMouseEnter={() => setIsCloseHovered(true)}
                    onMouseLeave={() => setIsCloseHovered(false)}
                >
                    <XIcon style={{
                        width: '1rem',
                        height: '1rem'
                    }} /> {/* size-4 */}

                    <span style={{
                        position: 'absolute',
                        width: '1px',
                        height: '1px',
                        padding: 0,
                        margin: '-1px',
                        overflow: 'hidden',
                        clip: 'rect(0, 0, 0, 0)',
                        whiteSpace: 'nowrap',
                        borderWidth: 0
                    }}>
                        Close
                    </span>
                </button>
            </div>
        </header>
    );
};

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

    // For handling responsive text alignment - moved before conditional return
    const [isSmallScreen, setIsSmallScreen] = React.useState(window.innerWidth < 640);

    // Move the resize effect before conditional return as well
    React.useEffect(() => {
        const handleResize = () => {
            setIsSmallScreen(window.innerWidth < 640);
        };

        window.addEventListener('resize', handleResize);
        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, []);

    if (!integration) {
        return (
            <main style={{ textAlign: 'center' }}>
                <h2 style={{
                    fontSize: '1.125rem', // text-lg
                    fontWeight: 600 // font-semibold
                }}>
                    Unable to Load Integration
                </h2>

                <p style={{
                    marginTop: '0.5rem', // mt-2
                    color: '#6b7280' // text-muted-foreground (using a gray color)
                }}>
                    We couldn't load the integration data. Please try again later.
                </p>

                <button
                    onClick={closeDialog}
                    style={{
                        marginTop: '1rem', // mt-4
                        display: 'inline-flex',
                        height: '2.5rem', // h-10
                        alignItems: 'center',
                        justifyContent: 'center',
                        borderRadius: '0.375rem', // rounded-md
                        border: '1px solid #e5e7eb', // border border-input
                        backgroundColor: '#fff', // bg-background
                        paddingLeft: '1rem', // px-4
                        paddingRight: '1rem',
                        paddingTop: '0.5rem', // py-2
                        paddingBottom: '0.5rem',
                        fontSize: '0.875rem', // text-sm
                        fontWeight: 500, // font-medium
                        transitionProperty: 'color, background-color, border-color',
                        transitionTimingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
                        transitionDuration: '150ms'
                    }}
                >
                    Close
                </button>
            </main>
        );
    }

    // For handling responsive text alignment
    const [isSmallScreen, setIsSmallScreen] = React.useState(window.innerWidth < 640);

    React.useEffect(() => {
        const handleResize = () => {
            setIsSmallScreen(window.innerWidth < 640);
        };

        window.addEventListener('resize', handleResize);
        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, []);

    return (
        <main style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem', // gap-4
            textAlign: isSmallScreen ? 'center' : 'left', // sm:text-left
            fontSize: '0.875rem' // text-sm
        }}>
            {dialogStep === 'initial' && <p style={{ color: '#6b7280' }}>{integration.description}</p>}

            {dialogStep === 'form' && (
                <form
                    id="form"
                    onSubmit={form.handleSubmit((data: any) => console.log(data))}
                    style={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '1rem', // gap-4
                        fontSize: '0.875rem' // text-sm
                    }}
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
}: DialogFooterProps) => {
    // For handling hover and focus states
    const [isCancelHovered, setIsCancelHovered] = React.useState(false);
    const [isSubmitHovered, setIsSubmitHovered] = React.useState(false);

    return (
        <footer style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: '0.5rem' // gap-2
        }}>
            <button
                type="button"
                style={{
                    display: 'inline-flex',
                    height: '2.5rem', // h-10
                    alignItems: 'center',
                    justifyContent: 'center',
                    borderRadius: '0.375rem', // rounded-md
                    border: '1px solid #e5e7eb', // border border-input
                    backgroundColor: isCancelHovered ? '#f3f4f6' : '#fff', // hover:bg-accent, bg-background
                    paddingLeft: '1rem', // px-4
                    paddingRight: '1rem',
                    paddingTop: '0.5rem', // py-2
                    paddingBottom: '0.5rem',
                    fontSize: '0.875rem', // text-sm
                    fontWeight: 500, // font-medium
                    color: isCancelHovered ? '#111827' : 'inherit', // hover:text-accent-foreground
                    transitionProperty: 'color, background-color, border-color',
                    transitionTimingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
                    transitionDuration: '150ms',
                    outline: 'none', // focus-visible:outline-none
                    cursor: 'pointer'
                }}
                onClick={closeDialog}
                onMouseEnter={() => setIsCancelHovered(true)}
                onMouseLeave={() => setIsCancelHovered(false)}
            >
                Cancel
            </button>

            <button
                autoFocus
                onClick={dialogStep === 'initial' ? handleContinue : handleSubmit}
                style={{
                    display: 'inline-flex',
                    height: '2.5rem', // h-10
                    alignItems: 'center',
                    justifyContent: 'center',
                    borderRadius: '0.375rem', // rounded-md
                    backgroundColor: isSubmitHovered ? 'rgba(0, 112, 243, 0.9)' : 'rgb(0, 112, 243)', // bg-bytechef, hover:bg-bytechef/90
                    paddingLeft: '1rem', // px-4
                    paddingRight: '1rem',
                    paddingTop: '0.5rem', // py-2
                    paddingBottom: '0.5rem',
                    fontSize: '0.875rem', // text-sm
                    fontWeight: 500, // font-medium
                    color: '#fff', // text-primary-foreground
                    transitionProperty: 'color, background-color, border-color',
                    transitionTimingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
                    transitionDuration: '150ms',
                    outline: 'none', // focus-visible:outline-none
                    cursor: 'pointer'
                }}
                type={dialogStep === 'form' ? 'submit' : 'button'}
                form="form"
                onMouseEnter={() => setIsSubmitHovered(true)}
                onMouseLeave={() => setIsSubmitHovered(false)}
            >
                {dialogStep === 'initial' && isOAuth2 && (
                    <span style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem' // gap-2
                    }}>
                        Authorize
                        <SquareArrowOutUpRightIcon style={{ width: '1rem', height: '1rem' }} /> {/* size-4 */}
                    </span>
                )}

                {dialogStep === 'initial' && !isOAuth2 && 'Continue'}

                {dialogStep !== 'initial' && 'Connect'}
            </button>
        </footer>
    );
};

const DialogPoweredBy = () => {
    // For handling hover state on link
    const [isLinkHovered, setIsLinkHovered] = React.useState(false);

    return (
        <div style={{
            position: 'absolute',
            bottom: '-2rem', // -bottom-8
            right: '50%',
            display: 'flex',
            transform: 'translateX(50%)', // translate-x-[50%]
            alignItems: 'center',
            justifyContent: 'center'
        }}>
            <img
                src={Logo}
                alt="ByteChef Logo"
                style={{
                    marginRight: '0.5rem', // mr-2
                    width: '1rem', // size-4
                    height: '1rem'
                }}
            />

            <span style={{
                fontSize: '0.875rem', // text-sm
                color: 'white' // text-white
            }}>
                Powered by
                <a
                    style={{
                        paddingLeft: '0.25rem', // pl-1
                        fontWeight: 600, // font-semibold
                        textDecoration: isLinkHovered ? 'underline' : 'none' // hover:underline
                    }}
                    href="https://bytechef.io"
                    target="_blank"
                    rel="noopener noreferrer"
                    onMouseEnter={() => setIsLinkHovered(true)}
                    onMouseLeave={() => setIsLinkHovered(false)}
                >
                    ByteChef
                </a>
            </span>
        </div>
    );
};

interface DialogInputFieldsetProps {
    label: string;
    name: string;
    placeholder?: string;
    options?: string[];
    required?: boolean;
    field?: any;
    error?: any;
}

const DialogInputField = ({label, name, options, placeholder, required, field, error}: DialogInputFieldsetProps) => {
    // For handling focus states
    const [isFocused, setIsFocused] = React.useState(false);

    return (
        <fieldset style={{ marginBottom: '0.5rem' }}>
            <div style={{ marginBottom: '0.5rem' }}>
                <label
                    htmlFor={name}
                    style={{
                        fontSize: '0.875rem', // text-sm
                        fontWeight: 500 // font-medium
                    }}
                >
                    {label}

                    {required && <span style={{
                        marginLeft: '0.125rem', // ml-0.5
                        lineHeight: '0.75rem', // leading-3
                        color: '#ef4444' // text-red-500
                    }}>*</span>}
                </label>
            </div>

            {options ? (
                <select
                    id={name}
                    style={{
                        display: 'flex',
                        height: '2.5rem', // h-10
                        width: '100%', // w-full
                        borderRadius: '0.375rem', // rounded-md
                        border: '1px solid #e5e7eb', // border border-input
                        backgroundColor: 'transparent', // bg-transparent
                        paddingLeft: '0.75rem', // px-3
                        paddingRight: '0.75rem',
                        paddingTop: '0.5rem', // py-2
                        paddingBottom: '0.5rem',
                        fontSize: '0.875rem', // text-sm
                        outline: isFocused ? 'none' : undefined, // focus-visible:outline-none
                        boxShadow: isFocused ? '0 0 0 2px #fff, 0 0 0 4px rgb(0, 112, 243)' : undefined, // focus-visible:ring-2 focus-visible:ring-bytechef
                        cursor: field?.disabled ? 'not-allowed' : 'default', // disabled:cursor-not-allowed
                        opacity: field?.disabled ? 0.5 : 1 // disabled:opacity-50
                    }}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
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
                    style={{
                        display: 'flex',
                        height: '2.5rem', // h-10
                        width: '100%', // w-full
                        borderRadius: '0.375rem', // rounded-md
                        border: '1px solid #e5e7eb', // border border-input
                        backgroundColor: 'transparent', // bg-transparent
                        paddingLeft: '0.75rem', // px-3
                        paddingRight: '0.75rem',
                        paddingTop: '0.5rem', // py-2
                        paddingBottom: '0.5rem',
                        fontSize: '0.875rem', // text-sm
                        outline: isFocused ? 'none' : undefined, // focus-visible:outline-none
                        boxShadow: isFocused ? '0 0 0 2px #fff, 0 0 0 4px rgb(0, 112, 243)' : undefined, // focus-visible:ring-2 focus-visible:ring-bytechef
                        cursor: field?.disabled ? 'not-allowed' : 'text', // disabled:cursor-not-allowed
                        opacity: field?.disabled ? 0.5 : 1 // disabled:opacity-50
                    }}
                    placeholder={placeholder}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    {...field}
                />
            )}

            {error && <p style={{
                marginTop: '0.25rem', // mt-1
                fontSize: '0.75rem', // text-xs
                color: '#ef4444' // text-red-500
            }}>{error.message}</p>}
        </fieldset>
    );
};

export default ConnectDialog;
