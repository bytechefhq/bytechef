import {useEffect, useState} from 'react';
import Logo from './assets/logo.svg';
import XIcon from './assets/x.svg';
import SquareArrowOutUpRightIcon from './assets/square-arrow-out-up-right.svg';
import styles from './styles.module.css';
import {
    FormType,
    IntegrationType,
    MergedMcpToolType,
    MergedWorkflowType,
    PropertyType,
    RegisterFormSubmitFunction,
    WorkflowInputType,
} from './types';

interface ToggleProps {
    id: string;
    pressed: boolean;
    onPressedChange: (pressed: boolean) => void;
}

const Toggle = ({id, pressed, onPressedChange}: ToggleProps) => (
    <>
        <input
            className={styles.toggleCheckbox}
            id={`react-switch-${id}`}
            type="checkbox"
            checked={pressed}
            onChange={(event) => onPressedChange(event.target.checked)}
        />

        <label
            className={styles.toggleLabel}
            htmlFor={`react-switch-${id}`}
            style={{
                backgroundColor: pressed ? '#1071e5' : '#e2e8f0',
            }}
        >
            <span className={styles.toggleButton} />
        </label>
    </>
);

interface DialogProps {
    closeDialog: () => void;
    workflowsView?: boolean;
    form?: FormType;
    handleClick: (event: React.MouseEvent<HTMLButtonElement>) => void;
    handleMcpToolToggle: (mcpToolId: number, pressed: boolean) => void;
    handleMcpWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleMcpWorkflowInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    handleWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleWorkflowInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    integration?: IntegrationType;
    isOAuth2?: boolean;
    isOpen: boolean;
    loading?: boolean;
    mergedMcpTools: MergedMcpToolType[];
    mergedMcpWorkflows: MergedWorkflowType[];
    mergedWorkflows: MergedWorkflowType[];
    properties?: PropertyType[];
    registerFormSubmit?: RegisterFormSubmitFunction;
}

const ConnectDialog = ({
    closeDialog,
    workflowsView = false,
    form,
    handleClick,
    handleMcpToolToggle,
    handleMcpWorkflowToggle,
    handleMcpWorkflowInputChange,
    handleWorkflowToggle,
    handleWorkflowInputChange,
    integration,
    isOAuth2 = false,
    isOpen,
    loading = false,
    mergedMcpTools,
    mergedMcpWorkflows,
    mergedWorkflows,
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
        <div className={styles.dialogOverlay} data-testid="dialog-overlay" onClick={closeDialog}>
            <div className={styles.dialogContainer} onClick={(event) => event.stopPropagation()}>
                <DialogHeader closeDialog={closeDialog} integration={integration} />

                {loading ? (
                    <main className={styles.dialogContentFallback}>
                        <p>Loading...</p>
                    </main>
                ) : integration ? (
                    <DialogContent
                        closeDialog={closeDialog}
                        workflowsView={workflowsView}
                        form={form}
                        handleMcpToolToggle={handleMcpToolToggle}
                        handleMcpWorkflowToggle={handleMcpWorkflowToggle}
                        handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}
                        handleWorkflowToggle={handleWorkflowToggle}
                        handleWorkflowInputChange={handleWorkflowInputChange}
                        integration={integration}
                        mergedMcpTools={mergedMcpTools}
                        mergedMcpWorkflows={mergedMcpWorkflows}
                        mergedWorkflows={mergedWorkflows}
                        properties={properties}
                        registerFormSubmit={registerFormSubmit}
                    />
                ) : (
                    <main className={styles.dialogContentFallback}>
                        <h2>Unable to Load Integration</h2>

                        <p>We couldn't load the integration data. Please try again later.</p>

                        <button className={styles.buttonSecondary} onClick={closeDialog}>
                            Close
                        </button>
                    </main>
                )}

                {integration && !loading && (
                    <DialogFooter workflowsView={workflowsView} handleClick={handleClick} isOAuth2={isOAuth2} />
                )}

                <DialogPoweredBy />
            </div>
        </div>
    );
};

interface DialogHeaderProps {
    closeDialog: () => void;
    integration?: IntegrationType;
}

const DialogHeader = ({closeDialog, integration}: DialogHeaderProps) => (
    <header className={styles.dialogHeader}>
        <div>
            {integration && integration.icon && (
                <div className={styles.integrationIcon} dangerouslySetInnerHTML={{__html: integration.icon}} />
            )}

            <h2>{integration?.name}</h2>
        </div>

        <button aria-label="Close Dialog" className={styles.closeButton} onClick={closeDialog}>
            <img src={XIcon} />
        </button>
    </header>
);

interface DialogWorkflowsContainerProps {
    handleWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleWorkflowInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    mergedWorkflows: MergedWorkflowType[];
}

const DialogWorkflowsContainer = ({
    handleWorkflowToggle,
    handleWorkflowInputChange,
    mergedWorkflows,
}: DialogWorkflowsContainerProps) => {
    return (
        <div data-testid="workflows-container" className={styles.workflowsContainer}>
            {mergedWorkflows?.length > 0 ? (
                <p>Enable, disable and manage your workflows below</p>
            ) : (
                <p>No workflows available for this integration.</p>
            )}

            <ul className={styles.workflowsList}>
                {mergedWorkflows.map((mergedWorkflow) => {
                    const {enabled = false, inputs, workflowUuid, label} = mergedWorkflow;

                    return (
                        <li key={workflowUuid}>
                            <div>
                                <span>{label}</span>

                                <Toggle
                                    id={workflowUuid}
                                    pressed={enabled}
                                    onPressedChange={(pressed) => handleWorkflowToggle(workflowUuid, pressed)}
                                />
                            </div>

                            {enabled && (
                                <div className={styles.workflowInputsContainer}>
                                    <span>INPUTS</span>

                                    {inputs?.length === 0 ? (
                                        <p className={styles.noInputsMessage}>No inputs defined for this workflow.</p>
                                    ) : (
                                        <ul>
                                            {inputs?.map((input: WorkflowInputType) => (
                                                <li key={input.name}>
                                                    <DialogInputField
                                                        onChange={(event) =>
                                                            handleWorkflowInputChange(
                                                                workflowUuid,
                                                                input.name,
                                                                event.target.value
                                                            )
                                                        }
                                                        label={input.label}
                                                        name={input.name}
                                                        required={input.required}
                                                        field={{value: input.value}}
                                                    />
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                            )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

interface DialogToolsContainerProps {
    handleMcpToolToggle: (mcpToolId: number, pressed: boolean) => void;
    handleMcpWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleMcpWorkflowInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    mergedMcpTools: MergedMcpToolType[];
    mergedMcpWorkflows: MergedWorkflowType[];
}

const DialogToolsContainer = ({
    handleMcpToolToggle,
    handleMcpWorkflowToggle,
    handleMcpWorkflowInputChange,
    mergedMcpTools,
    mergedMcpWorkflows,
}: DialogToolsContainerProps) => {
    return (
        <div data-testid="tools-container" className={styles.workflowsContainer}>
            {mergedMcpTools.length === 0 && mergedMcpWorkflows.length === 0 ? (
                <p>No tools available for this integration.</p>
            ) : (
                <p>Enable, disable and manage your tools below</p>
            )}

            {mergedMcpTools.length > 0 && (
                <ul className={styles.workflowsList}>
                    {mergedMcpTools.map((mcpTool) => (
                        <li key={mcpTool.id}>
                            <div>
                                <span>{mcpTool.label || mcpTool.name}</span>

                                <Toggle
                                    id={`mcp-tool-${mcpTool.id}`}
                                    pressed={mcpTool.enabled ?? false}
                                    onPressedChange={(pressed) => handleMcpToolToggle(mcpTool.id, pressed)}
                                />
                            </div>
                        </li>
                    ))}
                </ul>
            )}

            {mergedMcpWorkflows.length > 0 && (
                <ul className={styles.workflowsList}>
                    {mergedMcpWorkflows.map((mergedWorkflow) => {
                        const {enabled = false, inputs, label, workflowUuid} = mergedWorkflow;

                        return (
                            <li key={workflowUuid}>
                                <div>
                                    <span>{label}</span>

                                    <Toggle
                                        id={`mcp-workflow-${workflowUuid}`}
                                        pressed={enabled}
                                        onPressedChange={(pressed) =>
                                            handleMcpWorkflowToggle(workflowUuid, pressed)
                                        }
                                    />
                                </div>

                                {enabled && (
                                    <div className={styles.workflowInputsContainer}>
                                        <span>INPUTS</span>

                                        {inputs?.length === 0 ? (
                                            <p className={styles.noInputsMessage}>
                                                No inputs defined for this workflow.
                                            </p>
                                        ) : (
                                            <ul>
                                                {inputs?.map((input: WorkflowInputType) => (
                                                    <li key={input.name}>
                                                        <DialogInputField
                                                            onChange={(event) =>
                                                                handleMcpWorkflowInputChange(
                                                                    workflowUuid,
                                                                    input.name,
                                                                    event.target.value
                                                                )
                                                            }
                                                            label={input.label}
                                                            name={input.name}
                                                            required={input.required}
                                                            field={{value: input.value}}
                                                        />
                                                    </li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
};

interface DialogContentProps {
    closeDialog: () => void;
    workflowsView?: boolean;
    form?: FormType;
    handleMcpToolToggle: (mcpToolId: number, pressed: boolean) => void;
    handleMcpWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleMcpWorkflowInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    handleWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleWorkflowInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    integration: IntegrationType;
    mergedMcpTools: MergedMcpToolType[];
    mergedMcpWorkflows: MergedWorkflowType[];
    mergedWorkflows: MergedWorkflowType[];
    properties?: PropertyType[];
    registerFormSubmit?: RegisterFormSubmitFunction;
}

type TabType = 'tools' | 'workflows';

const DialogContent = ({
    workflowsView = false,
    form,
    handleMcpToolToggle,
    handleMcpWorkflowToggle,
    handleMcpWorkflowInputChange,
    handleWorkflowToggle,
    handleWorkflowInputChange,
    integration,
    mergedMcpTools,
    mergedMcpWorkflows,
    mergedWorkflows,
    properties,
    registerFormSubmit,
}: DialogContentProps) => {
    const [activeTab, setActiveTab] = useState<TabType>('workflows');

    const hasMcpContent = mergedMcpTools.length > 0 || mergedMcpWorkflows.length > 0;
    const showTabs = mergedWorkflows.length > 0 && hasMcpContent;

    // Register the form's submit handler when the component mounts
    useEffect(() => {
        if (registerFormSubmit && form) {
            registerFormSubmit(form.handleSubmit);
        }
    }, [registerFormSubmit, form]);

    return (
        <main className={styles.dialogContent}>
            {!workflowsView && <p>{integration.description}</p>}

            {!workflowsView && form && (
                <form id="form">
                    {properties?.map((property) => {
                        const field = form.register(property.name);

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

            {workflowsView && showTabs && (
                <div className={styles.tabContainer}>
                    <button
                        className={`${styles.tabButton} ${activeTab === 'workflows' ? styles.tabButtonActive : ''}`}
                        onClick={() => setActiveTab('workflows')}
                        type="button"
                    >
                        Workflows
                    </button>

                    <button
                        className={`${styles.tabButton} ${activeTab === 'tools' ? styles.tabButtonActive : ''}`}
                        onClick={() => setActiveTab('tools')}
                        type="button"
                    >
                        Tools
                    </button>
                </div>
            )}

            {workflowsView && (!showTabs || activeTab === 'workflows') && mergedWorkflows.length > 0 && (
                <DialogWorkflowsContainer
                    handleWorkflowToggle={handleWorkflowToggle}
                    handleWorkflowInputChange={handleWorkflowInputChange}
                    mergedWorkflows={mergedWorkflows}
                />
            )}

            {workflowsView && (!showTabs || activeTab === 'tools') && hasMcpContent && (
                <DialogToolsContainer
                    handleMcpToolToggle={handleMcpToolToggle}
                    handleMcpWorkflowToggle={handleMcpWorkflowToggle}
                    handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}
                    mergedMcpTools={mergedMcpTools}
                    mergedMcpWorkflows={mergedMcpWorkflows}
                />
            )}
        </main>
    );
};

interface DialogFooterProps {
    workflowsView?: boolean;
    handleClick: (event: React.MouseEvent<HTMLButtonElement>) => void;
    isOAuth2?: boolean;
}

const DialogFooter = ({workflowsView = false, handleClick, isOAuth2 = false}: DialogFooterProps) => (
    <footer className={styles.dialogFooter}>
        {workflowsView && (
            <button name="disconnectButton" onClick={handleClick} className={styles.buttonDestructive} type="button">
                Disconnect
            </button>
        )}

        {!workflowsView && (
            <button autoFocus onClick={handleClick} className={styles.buttonPrimary} type="button">
                {isOAuth2 ? (
                    <span>
                        Authorize
                        <img data-testid="authorize-icon" src={SquareArrowOutUpRightIcon} />
                    </span>
                ) : (
                    'Connect'
                )}
            </button>
        )}
    </footer>
);

const DialogPoweredBy = () => (
    <div className={styles.poweredByContainer}>
        <img src={Logo} alt="ByteChef Logo" />

        <span>
            Powered by
            <a href="https://bytechef.io" target="_blank" rel="noopener noreferrer">
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
    onChange?: (event: React.ChangeEvent<HTMLSelectElement | HTMLInputElement>) => void;
    required?: boolean;
    field?: React.InputHTMLAttributes<HTMLInputElement | HTMLSelectElement>;
    error?: {
        message: string;
    };
}

const DialogInputField = ({
    label,
    name,
    options,
    placeholder,
    required,
    field,
    error,
    onChange,
}: DialogInputFieldsetProps) => (
    <fieldset className={styles.dialogInputField}>
        <label htmlFor={name}>
            {label}

            {required && <span className={styles.requiredIndicator}>*</span>}
        </label>

        {options ? (
            <select id={name} {...field}>
                <option value="">Select {label}</option>

                {options.map((option) => (
                    <option key={option} value={option}>
                        {option}
                    </option>
                ))}
            </select>
        ) : (
            <input id={name} {...field} onChange={onChange} placeholder={placeholder} />
        )}

        {error && <span className={styles.inputError}>{error.message}</span>}
    </fieldset>
);

export default ConnectDialog;
