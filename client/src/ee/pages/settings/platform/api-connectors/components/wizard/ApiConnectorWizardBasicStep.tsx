import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import IconField from '@/ee/pages/settings/platform/api-connectors/components/IconField';
import {useEffect} from 'react';
import {useForm} from 'react-hook-form';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';

interface BasicStepFormDataI {
    baseUrl: string;
    icon: string;
    name: string;
}

const ApiConnectorWizardBasicStep = () => {
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
                    name="baseUrl"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Base URL</FormLabel>

                            <FormControl>
                                <Input placeholder="https://api.example.com/v1" {...field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />
            </div>
        </Form>
    );
};

export default ApiConnectorWizardBasicStep;
