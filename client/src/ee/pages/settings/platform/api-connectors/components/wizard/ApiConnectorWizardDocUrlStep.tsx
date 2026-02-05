import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import IconField from '@/ee/pages/settings/platform/api-connectors/components/IconField';

import useApiConnectorWizardDocUrlStep from './hooks/useApiConnectorWizardDocUrlStep';

const ApiConnectorWizardDocUrlStep = () => {
    const {control, form} = useApiConnectorWizardDocUrlStep();

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
                    name="documentationUrl"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Documentation URL</FormLabel>

                            <FormControl>
                                <Input placeholder="https://docs.example.com/api" {...field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: 'Documentation URL is required'}}
                />

                <FormField
                    control={control}
                    name="userPrompt"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Endpoint Instructions (Optional)</FormLabel>

                            <FormControl>
                                <Textarea
                                    className="min-h-[80px] resize-y"
                                    placeholder="Describe which endpoints you need, e.g., 'Only authentication and user management endpoints'"
                                    {...field}
                                />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <p className="text-sm text-muted-foreground">
                    Enter the URL of the API documentation. Our AI will analyze the documentation and generate an
                    OpenAPI specification for you.
                </p>
            </div>
        </Form>
    );
};

export default ApiConnectorWizardDocUrlStep;
