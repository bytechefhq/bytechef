import {Button} from '@/components/ui/button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {usePublishIntegrationMutation} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {IntegrationVersionKeys} from '@/ee/shared/queries/embedded/integrationVersions.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {useToast} from '@/hooks/use-toast';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {CircleDotIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    description: z.string().max(256).optional(),
});

const IntegrationHeaderPublishPopover = ({integration}: {integration: Integration}) => {
    const [open, setOpen] = useState(false);

    const {captureIntegrationPublished} = useAnalytics();

    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const publishIntegrationMutation = usePublishIntegrationMutation({
        onSuccess: () => {
            captureIntegrationPublished();

            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(integration.id!),
            });

            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.filteredIntegrations({}),
            });

            queryClient.invalidateQueries({
                queryKey: IntegrationVersionKeys.integrationIntegrationVersions(integration.id!),
            });

            toast({
                description: 'The integration has been published.',
            });

            reset({description: undefined});

            setOpen(false);
        },
    });

    function publishIntegration({description}: {description?: string}) {
        publishIntegrationMutation.mutate({
            id: integration.id!,
            publishIntegrationRequest: {
                description,
            },
        });
    }

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button className="hover:bg-background/70 [&_svg]:size-5" size="icon" variant="ghost">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <CircleDotIcon />
                        </TooltipTrigger>

                        <TooltipContent>Publish the integration</TooltipContent>
                    </Tooltip>
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="flex h-full w-96 flex-col justify-between space-y-4">
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(publishIntegration)}>
                        <h3 className="font-semibold">Publish Integration</h3>

                        <div className="flex-1">
                            <FormField
                                control={control}
                                name="description"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Description</FormLabel>

                                        <FormControl>
                                            <Textarea className="h-28" {...field}></Textarea>
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <div className="flex justify-end">
                            <Button size="sm" type="submit">
                                Publish
                            </Button>
                        </div>
                    </form>
                </Form>
            </PopoverContent>
        </Popover>
    );
};

export default IntegrationHeaderPublishPopover;
