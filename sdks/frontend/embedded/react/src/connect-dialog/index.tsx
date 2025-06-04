import ConnectDialog from './ConnectDialog';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import './styles.css';
import useOAuth2 from './useOAuth2';
import * as z from 'zod';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';

const JWT =
    'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNIVmliR2xqT21GTFpEWmFaMXBqTkhWcFRqUmhRa0pGV1daTlltVnFNMEZ1WVdkd1ltOU8ifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.jKFzMD3fynYAEPQbL0hrfuoIN86UwbIbv7FExEUCWhYRzYhEcUBa01sB4jFsxNt3wJe_QH1Y-NCGwbp2D4TvAoS7dCi4w9FoRdUuabRqELHlwvOEpHg5ebQ6xlSeGOtzvHZv7dDQ4_2ry5x85TKHZzdZ9UmC2NcRndTP65_Na89wO7LH6Adrr4mKCHyz_yHNuK4YHUeawM0bgNQaCCS03ivHzegRAAttWQF9oRxAIfs9-cv3VnKC030j9oTri6iK6w7YFWQpOIPp8PN83TrhdHTs2g1Q5SDzb44OiQv8NEBPVd018Ss61Yt2d0xoTj7usrIJxJH25qNirHSGTtJFpg';

const OAUTH2_TYPES = ['OAUTH2_AUTHORIZATION_CODE', 'OAUTH2_AUTHORIZATION_CODE_PKCE'];

export type DialogStepType = 'initial' | 'form';

interface ConnectionDialogHookReturnType {
    isOAuth2AuthorizationType: boolean;
    closeDialog: () => void;
    openDialog: () => void;
    DialogComponent: React.FC;
}

export default function useConnectDialog({
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

    const handleSubmit = useCallback(() => {
        if (formSubmitRef.current) {
            formSubmitRef.current(saveConnection);
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

    const formSchema = useMemo(
        () => createFormSchema(integration?.connectionConfig?.inputs || []),
        [integration?.connectionConfig?.inputs]
    );

    const form = useForm({
        resolver: zodResolver(formSchema),
        mode: 'onSubmit',
    });

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

        closeDialog();
    }

    const openDialog = async () => {
        await fetchIntegrationData();

        setIsOpen(true);
    };

    const closeDialog = () => {
        setDialogStep('initial');

        setIsOpen(false);
    };

    const handleContinue = () => {
        if (isOAuth2) {
            getAuth();
        } else {
            setDialogStep('form');
        }
    };

    const DialogComponent: React.FC = () => (
        <ConnectDialog
            closeDialog={closeDialog}
            dialogStep={dialogStep}
            form={form}
            handleContinue={handleContinue}
            handleSubmit={handleSubmit}
            integration={integration}
            isOAuth2={isOAuth2}
            isOpen={isOpen}
            properties={integration?.connectionConfig?.inputs}
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
