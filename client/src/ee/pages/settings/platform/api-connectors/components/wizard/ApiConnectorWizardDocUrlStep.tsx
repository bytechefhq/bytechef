import {Input} from '@/components/Input/Input';
import {Checkbox} from '@/components/ui/checkbox';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import IconField from '@/ee/pages/settings/platform/api-connectors/components/IconField';

import useApiConnectorWizard from './hooks/useApiConnectorWizard';

const ApiConnectorWizardDocUrlStep = () => {
    const {control, form} = useApiConnectorWizard('docUrl');

    const crawlEnabled = (form.watch('maxPages') ?? 1) > 1;

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

                <FormField
                    control={control}
                    name="maxPages"
                    render={({field}) => (
                        <FormItem>
                            <div className="flex items-center gap-2">
                                <Checkbox
                                    checked={crawlEnabled}
                                    id="crawl-enabled"
                                    onCheckedChange={(checked) => field.onChange(checked === true ? 10 : 1)}
                                />

                                <FormLabel className="!mt-0" htmlFor="crawl-enabled">
                                    Crawl linked documentation pages
                                </FormLabel>
                            </div>

                            {crawlEnabled && (
                                <FormControl>
                                    <Input
                                        max={50}
                                        min={2}
                                        onChange={(event) =>
                                            field.onChange(Math.max(2, Number(event.target.value) || 2))
                                        }
                                        type="number"
                                        value={field.value ?? 10}
                                    />
                                </FormControl>
                            )}

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
