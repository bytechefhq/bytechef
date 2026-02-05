import {useEffect} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';

type WizardFormType = 'basic' | 'docUrl' | 'import';

interface WizardFormDataI {
    baseUrl: string;
    documentationUrl: string;
    icon: string;
    name: string;
    specification: string;
    userPrompt: string;
}

interface UseApiConnectorWizardI {
    control: UseFormReturn<WizardFormDataI>['control'];
    form: UseFormReturn<WizardFormDataI>;
}

export default function useApiConnectorWizard(formType: WizardFormType): UseApiConnectorWizardI {
    const {baseUrl, documentationUrl, icon, name, specification, userPrompt} = useApiConnectorWizardStore();
    const {setBaseUrl, setDocumentationUrl, setIcon, setName, setSpecification, setUserPrompt} =
        useApiConnectorWizardStore();

    const form = useForm<WizardFormDataI>({
        defaultValues: {
            baseUrl: baseUrl || '',
            documentationUrl: documentationUrl || '',
            icon: icon || '',
            name: name || '',
            specification: specification || '',
            userPrompt: userPrompt || '',
        },
    });

    useEffect(() => {
        const subscription = form.watch((values) => {
            if (values.name !== undefined) {
                setName(values.name);
            }

            if (values.icon !== undefined) {
                setIcon(values.icon);
            }

            if (formType === 'basic' && values.baseUrl !== undefined) {
                setBaseUrl(values.baseUrl);
            }

            if (formType === 'import' && values.specification !== undefined) {
                setSpecification(values.specification);
            }

            if (formType === 'docUrl') {
                if (values.documentationUrl !== undefined) {
                    setDocumentationUrl(values.documentationUrl);
                }

                if (values.userPrompt !== undefined) {
                    setUserPrompt(values.userPrompt);
                }
            }
        });

        return () => subscription.unsubscribe();
    }, [form, formType, setBaseUrl, setDocumentationUrl, setIcon, setName, setSpecification, setUserPrompt]);

    return {control: form.control, form};
}
