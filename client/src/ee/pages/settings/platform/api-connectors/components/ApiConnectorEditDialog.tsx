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
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
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
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveApiConnector)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>Edit API Connector</DialogTitle>

                                <DialogDescription>Update the API connector configuration.</DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

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

export default ApiConnectorEditDialog;
