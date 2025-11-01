import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ApiCollectionEndpoint, HttpMethod} from '@/ee/shared/middleware/automation/api-platform';
import {
    useCreateApiCollectionEndpointMutation,
    useUpdateApiCollectionEndpointMutation,
} from '@/ee/shared/mutations/automation/apiCollectionEndpoints.mutations';
import {ApiCollectionKeys} from '@/ee/shared/mutations/automation/apiCollections.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    enabled: z.boolean(),
    httpMethod: z.custom<HttpMethod>((value) => !!value),
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
    path: z.string(),
    workflowUuid: z.string().min(1),
});

interface ApiEndpointDialogProps {
    apiCollectionId: number;
    apiEndpoint?: ApiCollectionEndpoint;
    collectionVersion: number;
    contextPath: string;
    onClose?: () => void;
    projectId: number;
    projectVersion: number;
    triggerNode?: ReactNode;
}

const ApiCollectionEndpointDialog = ({
    apiCollectionId,
    apiEndpoint,
    collectionVersion,
    contextPath,
    onClose,
    projectId,
    projectVersion,
    triggerNode,
}: ApiEndpointDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            enabled: apiEndpoint?.enabled || false,
            httpMethod: apiEndpoint?.httpMethod,
            name: apiEndpoint?.name || '',
            path: apiEndpoint?.path || '',
            workflowUuid: apiEndpoint?.workflowUuid || '',
        } as ApiCollectionEndpoint,
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const {data: workflows} = useGetProjectVersionWorkflowsQuery(projectId, projectVersion);

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ApiCollectionKeys.apiCollections,
        });

        closeDialog();
    };

    const createOpenApiEndpointMutation = useCreateApiCollectionEndpointMutation({onSuccess});
    const updateOpenApiEndpointMutation = useUpdateApiCollectionEndpointMutation({onSuccess});

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    };

    function saveOpenApiEndpoint() {
        if (apiEndpoint?.id) {
            updateOpenApiEndpointMutation.mutate({
                ...apiEndpoint,
                ...getValues(),
                apiCollectionId,
            });
        } else {
            createOpenApiEndpointMutation.mutate({
                ...apiEndpoint,
                ...getValues(),
                apiCollectionId,
            });
        }
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveOpenApiEndpoint)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{`${apiEndpoint?.id ? 'Edit' : 'Create'}`} API Endpoint</DialogTitle>

                                <DialogDescription>
                                    Create new API endpoint and connect it with a workflow.
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="workflowUuid"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Workflow</FormLabel>

                                    <FormControl>
                                        <Select
                                            disabled={!!apiEndpoint?.id}
                                            onValueChange={(value) => {
                                                field.onChange(value);

                                                if (!getValues('name')) {
                                                    setValue(
                                                        'name',
                                                        workflows?.find((workflow) => workflow.workflowUuid === value)
                                                            ?.label ?? ''
                                                    );
                                                }
                                            }}
                                            value={apiEndpoint?.workflowUuid}
                                        >
                                            <SelectTrigger className="w-full">
                                                <SelectValue placeholder="Select Workflow" />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {workflows &&
                                                    workflows.map((workflow) => (
                                                        <SelectItem
                                                            key={workflow.workflowUuid!}
                                                            value={workflow.workflowUuid!}
                                                        >
                                                            {workflow.label}
                                                        </SelectItem>
                                                    ))}
                                            </SelectContent>
                                        </Select>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="httpMethod"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>HTTP Method</FormLabel>

                                    <FormControl>
                                        <Select onValueChange={field.onChange} value={field.value}>
                                            <SelectTrigger className="w-full">
                                                <SelectValue placeholder="Select HTTP method" />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem
                                                    key={HttpMethod.Get.toString()}
                                                    value={HttpMethod.Get.toString()}
                                                >
                                                    GET
                                                </SelectItem>

                                                <SelectItem
                                                    key={HttpMethod.Patch.toString()}
                                                    value={HttpMethod.Patch.toString()}
                                                >
                                                    PATCH
                                                </SelectItem>

                                                <SelectItem
                                                    key={HttpMethod.Post.toString()}
                                                    value={HttpMethod.Post.toString()}
                                                >
                                                    POST
                                                </SelectItem>

                                                <SelectItem
                                                    key={HttpMethod.Put.toString()}
                                                    value={HttpMethod.Put.toString()}
                                                >
                                                    PUT
                                                </SelectItem>

                                                <SelectItem
                                                    key={HttpMethod.Delete.toString()}
                                                    value={HttpMethod.Delete.toString()}
                                                >
                                                    DELETE
                                                </SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="path"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Path</FormLabel>

                                    <FormControl>
                                        <div className="flex">
                                            <div className="flex h-9 items-center rounded-md rounded-r-none border border-r-0 border-input bg-muted px-3 py-1 text-sm shadow-sm transition-colors">
                                                {`/v${collectionVersion}/${contextPath}/`}
                                            </div>

                                            <Input {...field} className="rounded-l-none border-l-0" />
                                        </div>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ApiCollectionEndpointDialog;
