import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {IntegrationApi} from '@/ee/shared/middleware/embedded/configuration';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {CodeWorkflowLanguage, useCreateIntegrationCodeWorkflowMutation} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    componentName: z
        .string()
        .min(1, 'Component name is required')
        .max(256, 'Component name cannot be longer than 256 characters'),
    language: z.enum(CodeWorkflowLanguage),
});

interface NewIntegrationCodeWorkflowDialogProps {
    onClose: () => void;
}

const NewIntegrationCodeWorkflowDialog = ({onClose}: NewIntegrationCodeWorkflowDialogProps) => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            componentName: '',
            language: CodeWorkflowLanguage.Javascript,
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit} = form;

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const createIntegrationCodeWorkflowMutation = useCreateIntegrationCodeWorkflowMutation({
        onSuccess: async (data) => {
            const integrationId = Number(data.createIntegrationCodeWorkflow);

            const integration = await queryClient.fetchQuery({
                queryFn: () => new IntegrationApi().getIntegration({id: integrationId}),
                queryKey: IntegrationKeys.integration(integrationId),
            });

            onClose();

            navigate(
                `/embedded/integrations/${integrationId}/integration-workflows/${integration.integrationWorkflowIds![0]}`
            );
        },
    });

    const handleCreateIntegrationCodeWorkflow = () => {
        const {componentName, language} = getValues();

        createIntegrationCodeWorkflowMutation.mutate({componentName, language});
    };

    return (
        <Dialog onOpenChange={(open) => !open && !createIntegrationCodeWorkflowMutation.isPending && onClose()} open>
            <DialogContent
                onInteractOutside={(event) => createIntegrationCodeWorkflowMutation.isPending && event.preventDefault()}
            >
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleCreateIntegrationCodeWorkflow)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>New Code Workflow</DialogTitle>

                                <DialogDescription>
                                    Create a code-backed workflow and start editing its source.
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="componentName"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Component Name</FormLabel>

                                    <FormControl>
                                        <Input {...field} />
                                    </FormControl>

                                    <FormDescription>
                                        A unique name that identifies the integration this code workflow belongs to.
                                    </FormDescription>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="language"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Language</FormLabel>

                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                        </FormControl>

                                        <SelectContent>
                                            <SelectItem value={CodeWorkflowLanguage.Javascript}>JavaScript</SelectItem>

                                            <SelectItem value={CodeWorkflowLanguage.Python}>Python</SelectItem>

                                            <SelectItem value={CodeWorkflowLanguage.Ruby}>Ruby</SelectItem>
                                        </SelectContent>
                                    </Select>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <Button
                                disabled={createIntegrationCodeWorkflowMutation.isPending}
                                onClick={onClose}
                                type="button"
                                variant="ghost"
                            >
                                Cancel
                            </Button>

                            <Button disabled={createIntegrationCodeWorkflowMutation.isPending} type="submit">
                                Create
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default NewIntegrationCodeWorkflowDialog;
