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
import {useImportOpenApiSpecificationMutation} from '@/ee/mutations/apiConnector.mutations';
import IconField from '@/ee/pages/settings/platform/api-connectors/components/IconField';
import OpenApiSpecificationField from '@/ee/pages/settings/platform/api-connectors/components/OpenApiSpecificationField';
import {ApiConnectorKeys} from '@/ee/queries/apiConnectors.queries';
import {ApiConnector, ImportOpenApiSpecificationRequest} from '@/ee/shared/middleware/platform/api-connector';
import {useQueryClient} from '@tanstack/react-query';
import React, {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface ApiConnectorDialogProps {
    apiConnector?: ApiConnector;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const ApiConnectorImportDialog = ({apiConnector, onClose, triggerNode}: ApiConnectorDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm({
        defaultValues: {
            icon: apiConnector?.icon || '',
            name: apiConnector?.name || '',
            specification: apiConnector?.specification || '',
        } as ImportOpenApiSpecificationRequest,
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ApiConnectorKeys.apiConnectors,
        });

        closeDialog();
    };

    const importOpenApiSpecificationMutation = useImportOpenApiSpecificationMutation({onSuccess});

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function importOpenApiSpecification() {
        importOpenApiSpecificationMutation.mutate({
            ...apiConnector,
            ...getValues(),
        } as ImportOpenApiSpecificationRequest);
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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(importOpenApiSpecification)}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>{`${apiConnector?.id ? 'Edit' : 'Create'}`} API Connector</DialogTitle>
                            </div>

                            <DialogDescription>Create new API connector through the UI interface.</DialogDescription>
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input disabled={!!apiConnector?.id} {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
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
                            rules={{required: true}}
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

export default ApiConnectorImportDialog;
