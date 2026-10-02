import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import IconField from '@/ee/pages/settings/platform/api-connectors/components/IconField';
import OpenApiSpecificationField from '@/ee/pages/settings/platform/api-connectors/components/OpenApiSpecificationField';
import {ApiConnector} from '@/shared/middleware/graphql';

import useApiConnectorEditDialog from './hooks/useApiConnectorEditDialog';

interface ApiConnectorEditDialogProps {
    apiConnector: ApiConnector;
    onClose: () => void;
}

const ApiConnectorEditDialog = ({apiConnector, onClose}: ApiConnectorEditDialogProps) => {
    const {closeDialog, control, form, handleSubmit, isOpen, saveApiConnector} = useApiConnectorEditDialog({
        apiConnector,
        onClose,
    });

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            <DialogContent>
                <DialogMain>
                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveApiConnector)}>
                            <DialogHeader
                                description="Update the API connector configuration."
                                title="Edit API Connector"
                            />

                            <DialogBody>
                                <FormField
                                    control={control}
                                    name="name"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Name</FormLabel>

                                            <FormControl>
                                                <Input disabled {...field} />
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
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button type="submit">Save</Button>
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ApiConnectorEditDialog;
