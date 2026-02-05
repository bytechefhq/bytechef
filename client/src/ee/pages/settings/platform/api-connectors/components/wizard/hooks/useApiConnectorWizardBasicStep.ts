import {useEffect} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';

interface BasicStepFormDataI {
    baseUrl: string;
    icon: string;
    name: string;
}

interface UseApiConnectorWizardBasicStepI {
    control: UseFormReturn<BasicStepFormDataI>['control'];
    form: UseFormReturn<BasicStepFormDataI>;
}

export default function useApiConnectorWizardBasicStep(): UseApiConnectorWizardBasicStepI {
    const {baseUrl, icon, name, setBaseUrl, setIcon, setName} = useApiConnectorWizardStore();

    const form = useForm<BasicStepFormDataI>({
        defaultValues: {
            baseUrl: baseUrl || '',
            icon: icon || '',
            name: name || '',
        },
    });

    const {control, watch} = form;

    const watchedName = watch('name');
    const watchedIcon = watch('icon');
    const watchedBaseUrl = watch('baseUrl');

    useEffect(() => {
        setName(watchedName);
    }, [watchedName, setName]);

    useEffect(() => {
        setIcon(watchedIcon);
    }, [watchedIcon, setIcon]);

    useEffect(() => {
        setBaseUrl(watchedBaseUrl);
    }, [watchedBaseUrl, setBaseUrl]);

    return {
        control,
        form,
    };
}
