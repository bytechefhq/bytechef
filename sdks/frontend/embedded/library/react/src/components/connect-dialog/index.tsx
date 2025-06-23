import ConnectDialog from './ConnectDialog';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import useOAuth2 from './useOAuth2';
import {
    CodePayloadI,
    FormSubmitHandler,
    FormType,
    IntegrationType,
    PropertyType,
    RegisterFormSubmitFunction,
    TokenPayloadI,
    WorkflowInputType,
    WorkflowType,
} from './types';

const OAUTH2_TYPES = ['OAUTH2_AUTHORIZATION_CODE', 'OAUTH2_AUTHORIZATION_CODE_PKCE'];

interface ConnectionDialogHookReturnType {
    closeDialog: () => void;
    openDialog: () => void;
}

function createApiClient(baseUrl: string, environment: string, jwtToken: string) {
    const defaultHeaders = {
        Authorization: `Bearer ${jwtToken}`,
        'X-Environment': environment,
        'Content-Type': 'application/json',
    };

    return {
        async fetch<T>(
            endpoint: string,
            options: {
                method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
                body?: object;
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
            } catch (error: unknown) {
                console.error(`API Error (${endpoint}):`, (error as Error).message);

                throw error;
            }
        },
    };
}

function debounce<T extends (...args: unknown[]) => unknown>(fn: T, delay: number): (...args: Parameters<T>) => void {
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    return function (this: unknown, ...args: Parameters<T>) {
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
    environment?: string;
    integrationId: string;
    jwtToken: string;
}

export default function useConnectDialog({
    baseUrl = 'https://app.bytechef.io',
    environment = 'PRODUCTION',
    integrationId,
    jwtToken,
}: UseConnectDialogProps): ConnectionDialogHookReturnType {
    const [integration, setIntegration] = useState<IntegrationType | undefined>(undefined);
    const [isOAuth2, setIsOAuth2] = useState(false);
    const [isOpen, setIsOpen] = useState(false);
    const [formValues, setFormValues] = useState<Record<string, string>>({});
    const [formErrors, setFormErrors] = useState<Record<string, {message: string}>>({});
    const [selectedWorkflows, setSelectedWorkflows] = useState<string[]>([]);

    const inputRefs = useRef<Record<string, HTMLInputElement>>({});
    const portalContainerRef = useRef<HTMLElement | null>(null);
    const rootRef = useRef<ReturnType<typeof createRoot> | null>(null);
    const formSubmitRef = useRef<FormSubmitHandler | null>(null);

    const {fetch} = useMemo(() => createApiClient(baseUrl, environment, jwtToken), [baseUrl, environment, jwtToken]);

    const registerFormSubmit = useCallback<RegisterFormSubmitFunction>((submitFn) => {
        formSubmitRef.current = submitFn;
    }, []);

    const saveOAuth2Connection = useCallback(
        async (payload: CodePayloadI | TokenPayloadI) => {
            await fetch(`/api/embedded/v1/integrations/${integrationId}/instances`, {
                method: 'POST',
                body: {
                    connection: {
                        parameters: payload
                    }
                },
            });

            closeDialog();
        },
        [fetch, integrationId]
    );

    const saveNonOAuth2Connection = useCallback(
        async (formData: Record<string, string>) => {
            console.log('saveNonOAuth2Connection called with payload: ', formData);

            await fetch(`/api/embedded/v1/integrations/${integrationId}/instances`, {
                method: 'POST',
                body: {
                    connection: {
                        parameters: formData
                    }
                },
            });

            closeDialog();
        },
        [fetch, integrationId]
    );

    const handleSubmit = useCallback(() => {
        if (formSubmitRef.current) {
            const submitFunction = formSubmitRef.current(() => saveNonOAuth2Connection(formValues));

            submitFunction();
        }
    }, [formValues, saveNonOAuth2Connection]);

    const handleWorkflowToggle = useCallback(
        (workflowId: string, isSelected: boolean) => {
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
        },
        [fetch, integrationId]
    );

    const isOAuth2AuthorizationType = useMemo(
        () =>
            integration?.connectionConfig?.authorizationType &&
            OAUTH2_TYPES.includes(integration?.connectionConfig?.authorizationType),
        [integration]
    );

    const handleOnCodeSuccess = useCallback(
        (payload: CodePayloadI) => {
            if (payload.code) {
                saveOAuth2Connection(payload);
            }
        },
        [saveOAuth2Connection]
    );

    const handleOnTokenSuccess = useCallback(
        (payload: TokenPayloadI) => {
            if (payload.access_token) {
                saveOAuth2Connection(payload);
            }
        },
        [saveOAuth2Connection]
    );

    const {getAuth} = useOAuth2({
        ...integration?.connectionConfig?.oauth2,
        authorizationUrl: integration?.connectionConfig?.oauth2?.authorizationUrl || '',
        clientId: integration?.connectionConfig?.oauth2?.clientId || '',
        redirectUri: integration?.connectionConfig?.oauth2?.redirectUri || '',
        onCodeSuccess: handleOnCodeSuccess,
        onError: (error: string) => console.error(error),
        onTokenSuccess: handleOnTokenSuccess,
        responseType: isOAuth2AuthorizationType ? 'code' : 'token',
        scope: integration?.connectionConfig?.oauth2?.scopes?.join(' ') || '',
    });

    const createValidationRules = useCallback((properties: PropertyType[]): Record<string, ValidationRuleType> => {
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
    }, []);

    const validationRules = useMemo(
        () => createValidationRules(integration?.connectionConfig?.inputs || []),
        [createValidationRules, integration?.connectionConfig?.inputs]
    );

    const validateForm = useCallback((data: Record<string, string>, rules: Record<string, ValidationRuleType>) => {
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
    }, []);

    const form: FormType = useMemo(() => {
        return {
            register: (name: string) => ({
                name,
                defaultValue: formValues?.[name] || '',
                ref: (element: HTMLInputElement) => {
                    if (element) {
                        inputRefs.current[name] = element;
                    }
                },
                onInput: (event: React.FormEvent<HTMLInputElement>) => {
                    const value = event.currentTarget.value;

                    setFormValues((prev: CodePayloadI) => ({...prev, [name]: value}));
                },
            }),
            handleSubmit: (callback: (data: {[key: string]: unknown}) => void) => (event?: React.FormEvent) => {
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
    }, [formErrors, formValues, validateForm, validationRules]);

    interface ValidationRuleType {
        required: boolean;
        requiredMessage?: string;
    }

    const openDialog = async () => {
        setIsOpen(true);

        const integrationData: IntegrationType = await fetch(`/api/embedded/v1/integrations/${integrationId}`);

        setIntegration(integrationData);
    };

    const closeDialog = () => {
        setIsOpen(false);
    };

    const handleClick = useCallback(
        (event: React.MouseEvent<HTMLButtonElement>) => {
            if ((event.target as HTMLButtonElement).name === 'disconnectButton') {
                return;
            }

            if (isOAuth2) {
                console.log('invoking getAuth for OAuth2 flow');
                getAuth();
            } else {
                handleSubmit();
            }
        },
        [isOAuth2, getAuth, handleSubmit]
    );

    const debouncedFetchesRef = useRef<Record<string, (...args: unknown[]) => void>>({});

    const handleWorkflowInputChange = useCallback(
        (workflowReferenceCode: string, inputName: string, value: string) => {
            const matchingWorkflow = integration?.workflows?.find(
                (workflow: WorkflowType) => workflow.workflowReferenceCode === workflowReferenceCode
            );

            const matchingWorkflowInput = matchingWorkflow?.inputs?.find(
                (input: WorkflowInputType) => input.name === inputName
            );

            const body = {
                ...matchingWorkflow,
                inputs: [
                    ...(matchingWorkflow?.inputs as WorkflowInputType[]).filter(
                        (input: WorkflowInputType) => input.name !== inputName
                    ),
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
                            body: payload as object,
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
            rootRef.current.render(
                <ConnectDialog
                    closeDialog={closeDialog}
                    edit={false}
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
        form,
        formValues,
        handleClick,
        integration,
        isOAuth2,
        registerFormSubmit,
        handleWorkflowToggle,
        selectedWorkflows,
        handleWorkflowInputChange,
    ]);

    useEffect(() => {
        if (isOAuth2AuthorizationType) {
            setIsOAuth2(true);
        }
    }, [isOAuth2AuthorizationType]);

    return {
        openDialog,
        closeDialog,
    };
}
