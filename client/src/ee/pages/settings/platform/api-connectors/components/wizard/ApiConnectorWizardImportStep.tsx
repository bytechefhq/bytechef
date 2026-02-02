import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import IconField from '@/ee/pages/settings/platform/api-connectors/components/IconField';
import OpenApiSpecificationField from '@/ee/pages/settings/platform/api-connectors/components/OpenApiSpecificationField';
import {useEffect} from 'react';
import {useForm} from 'react-hook-form';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';

interface ImportStepFormDataI {
    icon: string;
    name: string;
    specification: string;
}

const ApiConnectorWizardImportStep = () => {
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

    return (
        <Form {...form}>
            <div className="flex flex-col gap-4 pb-4">
                <FormField
                    control={control}
                    name="name"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>

                            <FormControl>
                                <Input placeholder="my-api-connector" {...field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: 'Name is required'}}
                />

                <FormField
                    control={control}
                    name="icon"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Icon</FormLabel>

                            <FormControl>
                                <IconField field={field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={control}
                    name="specification"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Open API Specification</FormLabel>

                            <FormControl>
                                <OpenApiSpecificationField field={field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: 'Specification file is required'}}
                />
            </div>
        </Form>
    );
};

export default ApiConnectorWizardImportStep;
