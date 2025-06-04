import Dialog from './Dialog';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import './styles.css';
import useOAuth2 from './useOAuth2';
import * as z from 'zod';

const JWT =
    'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNIVmliR2xqT2pWU2FsbFVObEkwVW5nNFFuZFNRMlJUYlVGbE1VUnBhRms0ZVdwRlVHSnUifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.VniRtmGbk9VabhEXBvQxwhSwgBawNjzM5o4nqA-BsAaJuLnRqOeetYtVGDpEnYKpBAYSXh0chYX-2tjaRhiAG6IEoUo7eYKngGgm3rTtKYdJJUd0mbQTsQDZ_J9D5b48qBNOqlCAHp7H8KLyUNvorSs3VVaeBo4A4PUFkOlSU565XJjGLnvYFfX-wbZWbDtp9qa0cKjd2FKgqLj3PcssiYOyXXlIf3hVYFThBBVxOVlrSr15tmW1F76WkHOeb5UsuXzC_odTnIg1aVPdJsxku6dG0Q9EuhRzFSnXjTDeVgLRHGQwX9eaAZAMIzi2vnOtXdYJMKVuk6y71aY9qS1kUg';

const OAUTH2_TYPES = ['OAUTH2_AUTHORIZATION_CODE', 'OAUTH2_AUTHORIZATION_CODE_PKCE'];

export type DialogStepType = 'initial' | 'form';

interface ConnectionDialogHookReturnType {
    isOAuth2AuthorizationType: boolean;
    closeDialog: () => void;
    openDialog: () => void;
    DialogComponent: React.FC;
}

export default function useEmbeddedByteChefConnectionDialog({
    baseUrl = 'http://localhost:9555',
    integrationId = '1050',
}: {
    baseUrl?: string;
    integrationId?: string;
}): ConnectionDialogHookReturnType {
    const [dialogStep, setDialogStep] = useState<DialogStepType>('initial');
    const [integration, setIntegration] = useState<any>(null);
    const [isOAuth2, setIsOAuth2] = useState(false);
    const [isOpen, setIsOpen] = useState(false);

    const formSubmitRef = useRef<((data: any) => void) | null>(null);

    const registerFormSubmit = useCallback((submitFn: (data: any) => void) => {
        formSubmitRef.current = submitFn;
    }, []);

    const triggerFormSubmit = useCallback(() => {
        if (formSubmitRef.current) {
            formSubmitRef.current(handleSubmit);
        }
    }, []);

    const isOAuth2AuthorizationType = useMemo(
        () => OAUTH2_TYPES.includes(integration?.connectionConfig?.authorizationType),
        [integration]
    );

    const {getAuth} = useOAuth2({
        ...integration?.connectionConfig?.oauth2,
        onCodeSuccess: () => console.log('onCodeSuccess called'),
        onError: (error: string) => console.error(error),
        onTokenSuccess: () => console.log('onTokenSuccess called'),
        responseType: isOAuth2AuthorizationType ? 'code' : 'token',
        scope: integration?.connectionConfig?.oauth2?.scopes?.join(' '),
    });

    // Dynamically generate the form schema from integration properties
    function createFormSchema(properties: any[]) {
        if (!properties || properties.length === 0) {
            return z.object({});
        }

        const schema: Record<string, z.ZodTypeAny> = {};

        properties.forEach((prop) => {
            if (prop.required) {
                schema[prop.name] = z.string().min(1, {message: `${prop.label} is required`});
            } else {
                schema[prop.name] = z.string().optional();
            }
        });

        return z.object(schema);
    }

    async function fetchIntegrationData() {
        const url = `${baseUrl}/api/embedded/v1/integrations/${integrationId}`;

        try {
            const response = await fetch(url, {
                headers: {
                    Authorization: `Bearer ${JWT}`,
                    'x-environment': 'development',
                },
            });

            if (!response.ok) {
                throw new Error(`Response status: ${response.status}`);
            }

            const integrationData = await response.json();

            setIntegration(integrationData);
        } catch (error: any) {
            console.error(error?.message);
        }
    }

    async function saveConnection() {
        console.log('saveConnection called');
    }

    const openDialog = async () => {
        await fetchIntegrationData();

        setIsOpen(true);
    };

    const closeDialog = () => {
        setDialogStep('initial');

        setIsOpen(false);
    };

    const handleSubmit = () => {
        saveConnection();

        closeDialog();
    };

    const handleContinue = () => {
        if (isOAuth2) {
            getAuth();
        } else {
            setDialogStep('form');
        }
    };

    const DialogComponent: React.FC = () => (
        <Dialog
            closeDialog={closeDialog}
            dialogStep={dialogStep}
            formSchema={createFormSchema(integration?.connectionConfig?.inputs)}
            handleContinue={handleContinue}
            handleSubmit={handleSubmit}
            integration={integration}
            isOAuth2={false}
            isOpen={isOpen}
            properties={integration?.connectionConfig?.inputs}
            triggerFormSubmit={triggerFormSubmit}
            registerFormSubmit={registerFormSubmit}
        />
    );

    useEffect(() => {
        if (integration?.connectionConfig?.authorizationType.startsWith('OAUTH2')) {
            setIsOAuth2(true);
        }
    }, [integration?.connectionConfig?.authorizationType]);

    return {
        isOAuth2AuthorizationType,
        openDialog,
        closeDialog,
        DialogComponent,
    };
}
