import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {useCreateApiCollectionMutation, useUpdateApiCollectionMutation} from '@/ee/mutations/apiCollections.mutations';
import {ApiCollectionKeys} from '@/ee/queries/apiCollections.queries';
import {ApiCollection} from '@/ee/shared/middleware/automation/api-platform';
import ProjectInstanceDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-instances/components/project-instance-dialog/ProjectInstanceDialogBasicStepProjectVersionsSelect';
import ProjectInstanceDialogBasicStepProjectsComboBox from '@/pages/automation/project-instances/components/project-instance-dialog/ProjectInstanceDialogBasicStepProjectsComboBox';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import React, {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    collectionVersion: z.coerce.number().min(1),
    description: z.string(),
    enabled: z.boolean().default(false),
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
    projectId: z.number().min(1),
    projectVersion: z.number().min(1),
    workspaceId: z.number(),
});

interface ApiCollectionDialogProps {
    apiCollection?: ApiCollection;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const ApiCollectionDialog = ({apiCollection, onClose, triggerNode}: ApiCollectionDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [curProjectId, setCurProjectId] = useState<number | undefined>(apiCollection?.projectId);
    const [curProjectVersion, setCurProjectVersion] = useState<number | undefined>(apiCollection?.projectVersion);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            collectionVersion: apiCollection?.collectionVersion || 1,
            description: apiCollection?.description || '',
            name: apiCollection?.name || '',
            projectId: apiCollection?.projectId,
            projectVersion: apiCollection?.projectVersion,
            workspaceId: -1,
        } as ApiCollection,
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset, resetField, setValue} = form;

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ApiCollectionKeys.apiCollections,
        });

        closeDialog();
    };

    const createOpenApiCollectionMutation = useCreateApiCollectionMutation({onSuccess});
    const updateOpenApiCollectionMutation = useUpdateApiCollectionMutation({onSuccess});

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    };

    function saveOpenApiCollection() {
        console.log(apiCollection);
        if (apiCollection?.id) {
            updateOpenApiCollectionMutation.mutate({
                ...apiCollection,
                ...getValues(),
            });
        } else {
            createOpenApiCollectionMutation.mutate({
                ...apiCollection,
                ...getValues(),
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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveOpenApiCollection)}>
                        <DialogHeader>
                            <DialogTitle>{`${apiCollection?.id ? 'Edit' : 'Create'}`} API Collection</DialogTitle>

                            <DialogDescription>
                                Create new API collection and connect it with a project.
                            </DialogDescription>
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="projectId"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Project</FormLabel>

                                    <FormControl>
                                        <ProjectInstanceDialogBasicStepProjectsComboBox
                                            onBlur={field.onBlur}
                                            onChange={(item) => {
                                                if (item) {
                                                    setValue('projectId', item.value);
                                                    resetField('projectVersion');

                                                    if (!getValues('name')) {
                                                        setValue('name', item.name!.toString());
                                                    }

                                                    setCurProjectId(item.value);
                                                    setCurProjectVersion(undefined);
                                                }
                                            }}
                                            value={field.value}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            shouldUnregister={false}
                        />

                        {curProjectId && (
                            <FormField
                                control={control}
                                name="projectVersion"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Project Version</FormLabel>

                                        <FormControl>
                                            <ProjectInstanceDialogBasicStepProjectVersionsSelect
                                                onChange={(value) => {
                                                    field.onChange(value);
                                                    setCurProjectVersion(value);
                                                }}
                                                projectId={curProjectId}
                                                projectVersion={curProjectVersion}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                shouldUnregister={false}
                            />
                        )}

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
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="collectionVersion"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Collection Version</FormLabel>

                                    <FormControl>
                                        <Input type="number" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            shouldUnregister={false}
                        />

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">Save</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ApiCollectionDialog;
