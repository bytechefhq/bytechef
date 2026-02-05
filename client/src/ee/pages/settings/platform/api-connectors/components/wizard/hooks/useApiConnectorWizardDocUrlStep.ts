import {useEffect} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';

interface DocUrlStepFormDataI {
    documentationUrl: string;
    icon: string;
    name: string;
    userPrompt: string;
}

interface UseApiConnectorWizardDocUrlStepI {
    control: UseFormReturn<DocUrlStepFormDataI>['control'];
    form: UseFormReturn<DocUrlStepFormDataI>;
}

export default function useApiConnectorWizardDocUrlStep(): UseApiConnectorWizardDocUrlStepI {
    const {documentationUrl, icon, name, setDocumentationUrl, setIcon, setName, setUserPrompt, userPrompt} =
        useApiConnectorWizardStore();

    const form = useForm<DocUrlStepFormDataI>({
        defaultValues: {
            documentationUrl: documentationUrl || '',
            icon: icon || '',
            name: name || '',
            userPrompt: userPrompt || '',
        },
    });

    const {control, watch} = form;

    const watchedName = watch('name');
    const watchedIcon = watch('icon');
    const watchedDocumentationUrl = watch('documentationUrl');
    const watchedUserPrompt = watch('userPrompt');

    useEffect(() => {
        setName(watchedName);
    }, [watchedName, setName]);

    useEffect(() => {
        setIcon(watchedIcon);
    }, [watchedIcon, setIcon]);

    useEffect(() => {
        setDocumentationUrl(watchedDocumentationUrl);
    }, [watchedDocumentationUrl, setDocumentationUrl]);

    useEffect(() => {
        setUserPrompt(watchedUserPrompt);
    }, [watchedUserPrompt, setUserPrompt]);

    return {
        control,
        form,
    };
}
