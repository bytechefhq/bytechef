import ConnectDialog from './ConnectDialog';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import useOAuth2 from './useOAuth2';

const OAUTH2_TYPES = ['OAUTH2_AUTHORIZATION_CODE', 'OAUTH2_AUTHORIZATION_CODE_PKCE'];

export const DIALOG_STEPS = {
    initial: 'Description',
    workflows: 'Workflows',
    form: 'Custom Data',
} as const;

export type DialogStepKeyType = keyof typeof DIALOG_STEPS;

export interface DialogStepType {
    key: DialogStepKeyType;
    label: string;
}

interface ConnectionDialogHookReturnType {
    isOAuth2AuthorizationType: boolean;
    closeDialog: () => void;
    openDialog: () => void;
}

function createApiClient(baseUrl: string, jwtToken: string) {
    const defaultHeaders = {
        Authorization: `Bearer ${jwtToken}`,
        'x-environment': 'development',
        'Content-Type': 'application/json',
    };

    return {
        async fetch<T>(
            endpoint: string,
            options: {
                method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
                body?: any;
                headers?: Record<string, string>;
            } = {}
        ): Promise<T> {
            const {method = 'GET', body, headers = {}} = options;
            const url = `${baseUrl}${endpoint}`;

            try {
                const response = await fetch(url, {
                    method,
                    headers: {...defaultHeaders, ...headers},
                    ...(body && {body: JSON.stringify(body)}),
                });

                if (!response.ok) {
                    throw new Error(`Response status: ${response.status}`);
                }

                // Only try to parse JSON if we expect a response body
                if (method === 'GET' || response.headers.get('content-length') !== '0') {
                    return await response.json();
                }

                return {} as T;
            } catch (error: any) {
                console.error(`API Error (${endpoint}):`, error?.message);
                throw error;
            }
        },
    };
}

function debounce<T extends (...args: any[]) => any>(fn: T, delay: number): (...args: Parameters<T>) => void {
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    return function (this: any, ...args: Parameters<T>) {
        if (timeoutId) {
            clearTimeout(timeoutId);
        }

        timeoutId = setTimeout(() => {
            fn.apply(this, args);

            timeoutId = null;
        }, delay);
    };
}

interface UseConnectDialogProps {
    baseUrl?: string;
    integrationId: string;
    jwtToken: string;
}

export default function useConnectDialog({
    baseUrl = 'http://localhost:9555',
    integrationId,
    jwtToken,
}: UseConnectDialogProps): ConnectionDialogHookReturnType {
    const [dialogStep, setDialogStep] = useState<DialogStepKeyType>('initial');
    const [integration, setIntegration] = useState<any>(null);
    const [isOAuth2, setIsOAuth2] = useState(false);
    const [isOpen, setIsOpen] = useState(false);
    const [formValues, setFormValues] = useState<Record<string, string>>({});
    const [formErrors, setFormErrors] = useState<Record<string, {message: string}>>({});
    const [selectedWorkflows, setSelectedWorkflows] = useState<string[]>([]);

    const inputRefs = useRef<Record<string, HTMLInputElement>>({});
    const portalContainerRef = useRef<HTMLElement | null>(null);
    const rootRef = useRef<ReturnType<typeof createRoot> | null>(null);
    const formSubmitRef = useRef<((data: any) => void) | null>(null);

    const {fetch} = useMemo(() => createApiClient(baseUrl, jwtToken), [baseUrl, jwtToken]);

    const registerFormSubmit = useCallback((submitFn: (data: any) => void) => {
        formSubmitRef.current = submitFn;
    }, []);

    const handleSubmit = useCallback(() => {
        if (formSubmitRef.current) {
            const submitFunction = formSubmitRef.current(saveConnection);

            submitFunction();
        }
    }, []);

    const handleWorkflowToggle = useCallback((workflowId: string, isSelected: boolean) => {
        if (isSelected) {
            setSelectedWorkflows((selectedWorkflows) => [...selectedWorkflows, workflowId]);

            fetch(`/api/embedded/v1/integration-instances/${integrationId}/workflows/${workflowId}/enable`, {
                method: 'POST',
            });
        } else {
            fetch(`/api/embedded/v1/integration-instances/${integrationId}/workflows/${workflowId}/enable`, {
                method: 'DELETE',
            });
            setSelectedWorkflows((selectedWorkflows) => selectedWorkflows.filter((id) => id !== workflowId));
        }
    }, []);

    const isOAuth2AuthorizationType = useMemo(
        () => OAUTH2_TYPES.includes(integration?.connectionConfig?.authorizationType),
        [integration]
    );

    const {getAuth} = useOAuth2({
        ...integration?.connectionConfig?.oauth2,
        onCodeSuccess: (payload) => console.log('onCodeSuccess called: ', payload),
        onError: (error: string) => console.error(error),
        onTokenSuccess: (payload) => console.log('onTokenSuccess called: ', payload),
        responseType: isOAuth2AuthorizationType ? 'code' : 'token',
        scope: integration?.connectionConfig?.oauth2?.scopes?.join(' '),
    });

    const validationRules = useMemo(
        () => createValidationRules(integration?.connectionConfig?.inputs || []),
        [integration?.connectionConfig?.inputs]
    );

    const form = useMemo(() => {
        return {
            register: (name: string) => ({
                name,
                defaultValue: formValues[name] || '',
                ref: (element: HTMLInputElement) => {
                    if (element) {
                        inputRefs.current[name] = element;
                    }
                },
                onInput: (event: React.FormEvent<HTMLInputElement>) => {
                    const value = event.currentTarget.value;

                    setFormValues((prev) => ({...prev, [name]: value}));
                },
            }),
            handleSubmit: (callback: (data: any) => void) => (event?: React.FormEvent) => {
                if (event) {
                    event.preventDefault();
                }

                const currentValues = Object.entries(inputRefs.current).reduce(
                    (acc, [name, ref]) => {
                        acc[name] = ref.value;

                        return acc;
                    },
                    {} as Record<string, string>
                );

                const {isValid, errors, validatedData} = validateForm(currentValues, validationRules);

                if (isValid) {
                    setFormErrors({});

                    callback(validatedData);

                    return true;
                } else {
                    setFormErrors(errors);

                    return false;
                }
            },
            formState: {
                errors: formErrors,
            },
        };
    }, [validationRules, formErrors]);

    function validateForm(data: Record<string, string>, rules: Record<string, ValidationRuleType>) {
        const errors: Record<string, {message: string}> = {};
        const validatedData: Record<string, string> = {};

        Object.keys(rules).forEach((fieldName) => {
            const rule = rules[fieldName];
            const value = data[fieldName] || '';

            if (rule.required && value.trim() === '') {
                errors[fieldName] = {message: rule.requiredMessage || 'This field is required'};
            } else {
                validatedData[fieldName] = value;
            }
        });

        return {
            isValid: Object.keys(errors).length === 0,
            errors,
            validatedData,
        };
    }

    interface ValidationRuleType {
        required: boolean;
        requiredMessage?: string;
    }

    function createValidationRules(properties: any[]): Record<string, ValidationRuleType> {
        if (!properties || properties.length === 0) {
            return {};
        }

        const rules: Record<string, ValidationRuleType> = {};

        properties.forEach((prop) => {
            rules[prop.name] = {
                required: !!prop.required,
                requiredMessage: prop.required ? `${prop.label} is required` : undefined,
            };
        });

        return rules;
    }

    async function saveConnection() {
        await fetch(`/api/embedded/v1/integrations/${integrationId}/instances`, {
            method: 'POST',
            body: formValues,
        });

        closeDialog();
    }

    const openDialog = async () => {
        setIsOpen(true);

        const integrationData = await fetch(`/api/embedded/v1/integrations/${integrationId}`);

        setIntegration(integrationData);
    };

    const closeDialog = () => {
        setDialogStep('initial');

        setIsOpen(false);
    };

    const getCurrentStep = useCallback((): DialogStepType => {
        return {
            key: dialogStep,
            label: DIALOG_STEPS[dialogStep],
        };
    }, [dialogStep]);

    const handleClick = useCallback(
        (event: React.MouseEvent<HTMLButtonElement>) => {
            if ((event.target as HTMLButtonElement).name === 'backButton') {
                setDialogStep('workflows');

                return;
            }

            if (dialogStep === 'initial') {
                setDialogStep('workflows');
            } else if (dialogStep === 'workflows') {
                if (isOAuth2) {
                    getAuth();
                } else {
                    setDialogStep('form');
                }
            } else if (dialogStep === 'form') {
                handleSubmit();
            }
        },
        [dialogStep, isOAuth2, getAuth, handleSubmit]
    );

    const debouncedFetchesRef = useRef<Record<string, (...args: any[]) => void>>({});

    const handleWorkflowInputChange = useCallback(
        (workflowReferenceCode: string, inputName: string, value: string) => {
            const matchingWorkflow = integration?.workflows?.find(
                (workflow: any) => workflow.workflowReferenceCode === workflowReferenceCode
            );

            const matchingWorkflowInput = matchingWorkflow?.inputs?.find((input: any) => input.name === inputName);

            const body = {
                ...matchingWorkflow,
                inputs: [
                    ...matchingWorkflow.inputs.filter((input: any) => input.name !== inputName),
                    {
                        ...matchingWorkflowInput,
                        value: value,
                    },
                ],
            };

            const debouncedFetchKey = `${workflowReferenceCode}_${inputName}`;

            if (!debouncedFetchesRef.current[debouncedFetchKey]) {
                debouncedFetchesRef.current[debouncedFetchKey] = debounce((payload) => {
                    fetch(
                        `/api/embedded/v1/integration-instances/${integrationId}/workflows/${workflowReferenceCode}`,
                        {
                            body: payload,
                            method: 'PUT',
                        }
                    );
                }, 500);
            }

            debouncedFetchesRef.current[debouncedFetchKey](body);
        },
        [fetch, integration?.workflows, integrationId]
    );

    // Create portal container only once
    useEffect(() => {
        let container = document.getElementById('connect-dialog-portal');

        if (!container) {
            container = document.createElement('div');

            container.id = 'connect-dialog-portal';

            document.body.appendChild(container);
            portalContainerRef.current = container;
        }

        // Cleanup on unmount
        return () => {
            if (portalContainerRef.current) {
                document.body.removeChild(portalContainerRef.current);
            }
        };
    }, []);

    // Handle creation and updates
    useEffect(() => {
        // Clean up when closed
        if (!isOpen) {
            if (rootRef.current) {
                rootRef.current.unmount();

                rootRef.current = null;
            }
            return;
        }

        // Create root if needed
        if (!rootRef.current && portalContainerRef.current) {
            rootRef.current = createRoot(portalContainerRef.current);
        }

        // Always render with current state
        if (rootRef.current) {
            const currentStep = getCurrentStep();

            rootRef.current.render(
                <ConnectDialog
                    closeDialog={closeDialog}
                    dialogStep={dialogStep} // Keep this as just the key for now
                    dialogStepLabel={currentStep.label} // Add the label
                    form={form}
                    handleWorkflowToggle={handleWorkflowToggle}
                    handleWorkflowInputChange={handleWorkflowInputChange}
                    handleClick={handleClick}
                    integration={integration}
                    isOAuth2={isOAuth2}
                    isOpen={isOpen}
                    properties={integration?.connectionConfig?.inputs}
                    registerFormSubmit={registerFormSubmit}
                    selectedWorkflows={selectedWorkflows}
                />
            );
        }
    }, [
        isOpen,
        dialogStep,
        form,
        formValues,
        handleClick,
        integration,
        isOAuth2,
        registerFormSubmit,
        handleWorkflowToggle,
        selectedWorkflows,
        getCurrentStep,
        handleWorkflowInputChange,
    ]);

    useEffect(() => {
        if (integration?.connectionConfig?.authorizationType.startsWith('OAUTH2')) {
            setIsOAuth2(true);
        }
    }, [integration?.connectionConfig?.authorizationType]);

    return {
        isOAuth2AuthorizationType,
        openDialog,
        closeDialog,
    };
}
