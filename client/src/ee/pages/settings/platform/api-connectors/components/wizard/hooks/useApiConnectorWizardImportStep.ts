import {useEffect} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';

interface ImportStepFormDataI {
    icon: string;
    name: string;
    specification: string;
}

interface UseApiConnectorWizardImportStepI {
    control: UseFormReturn<ImportStepFormDataI>['control'];
    form: UseFormReturn<ImportStepFormDataI>;
}

export default function useApiConnectorWizardImportStep(): UseApiConnectorWizardImportStepI {
    const {icon, name, setIcon, setName, setSpecification, specification} = useApiConnectorWizardStore();

    const form = useForm<ImportStepFormDataI>({
        defaultValues: {
            icon: icon || '',
            name: name || '',
            specification: specification || '',
        },
    });

    const {control, watch} = form;

    const watchedName = watch('name');
    const watchedIcon = watch('icon');
    const watchedSpecification = watch('specification');

    useEffect(() => {
        setName(watchedName);
    }, [watchedName, setName]);

    useEffect(() => {
        setIcon(watchedIcon);
    }, [watchedIcon, setIcon]);

    useEffect(() => {
        setSpecification(watchedSpecification);
    }, [watchedSpecification, setSpecification]);

    return {
        control,
        form,
    };
}
