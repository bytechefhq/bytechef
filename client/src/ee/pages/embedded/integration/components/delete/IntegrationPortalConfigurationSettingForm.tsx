import Button from '@/components/Button/Button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Switch} from '@/components/ui/switch';
import {toast} from '@/hooks/use-toast';
import {zodResolver} from '@hookform/resolvers/zod';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    label: z.string().min(2),
    name: z.string().min(2),
    required: z.number(),
    tooltip: z.string().min(2),
    type: z.string().min(2),
});

export function IntegrationPortalConfigurationSettingForm() {
    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    function handleSubmit(data: z.infer<typeof formSchema>) {
        toast({
            description: (
                <pre className="mt-2 w-[340px] rounded-md bg-slate-950 p-4">
                    <code className="text-white">{JSON.stringify(data, null, 4)}</code>
                </pre>
            ),
            title: 'You submitted the following values:',
        });
    }

    return (
        <Form {...form}>
            <form className="flex w-full flex-col gap-y-4" onSubmit={form.handleSubmit(handleSubmit)}>
                <FormField
                    control={form.control}
                    name="name"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>

                            <FormControl>
                                <Input {...field} />
                            </FormControl>

                            <FormDescription></FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="label"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Label</FormLabel>

                            <FormControl>
                                <Input {...field} />
                            </FormControl>

                            <FormDescription></FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="type"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Type</FormLabel>

                            <Select defaultValue={field.value} onValueChange={field.onChange}>
                                <FormControl>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select type" />
                                    </SelectTrigger>
                                </FormControl>

                                <SelectContent>
                                    <SelectItem value="string">String</SelectItem>

                                    <SelectItem value="number">Number</SelectItem>

                                    <SelectItem value="boolean">Boolean</SelectItem>

                                    <SelectItem value="date">Date</SelectItem>
                                </SelectContent>
                            </Select>

                            <FormDescription></FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="tooltip"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Tooltip</FormLabel>

                            <FormControl>
                                <Input {...field} />
                            </FormControl>

                            <FormDescription></FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="required"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Required</FormLabel>

                            <FormControl>
                                <Switch {...field} />
                            </FormControl>

                            <FormDescription></FormDescription>

                            <FormMessage />
                        </FormItem>
                    )}
                />

                <Button type="submit">Save</Button>
            </form>
        </Form>
    );
}

export default IntegrationPortalConfigurationSettingForm;
