import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import useVariables from '@/ee/shared/components/variables/hooks/useVariables';
import {useVariablesStore} from '@/ee/shared/components/variables/stores/useVariablesStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {ReactNode, useState} from 'react';
import {useForm, useWatch} from 'react-hook-form';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

const formSchema = z.object({
    name: z
        .string()
        .min(1, 'Name is required')
        .max(50, 'Name cannot be longer than 50 characters')
        .regex(
            /^[A-Za-z_][A-Za-z0-9_]*$/,
            'Name must start with a letter or underscore and contain only letters, digits and underscores'
        ),
    value: z.string().max(4096, 'Value cannot be longer than 4096 characters'),
});

interface VariableDialogProps {
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const VariableDialog = ({onClose, triggerNode}: VariableDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const {currentVariable, setCurrentVariable, setShowEditDialog} = useVariablesStore(
        useShallow((state) => ({
            currentVariable: state.currentVariable,
            setCurrentVariable: state.setCurrentVariable,
            setShowEditDialog: state.setShowEditDialog,
        }))
    );

    const {handleSave} = useVariables();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: currentVariable?.name || '',
            value: currentVariable?.value || '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const watchedName = useWatch({control, name: 'name'});

    function closeDialog() {
        reset();
        setCurrentVariable(undefined);
        setShowEditDialog(false);
        setIsOpen(false);

        onClose?.();
    }

    function saveVariable(values: z.infer<typeof formSchema>) {
        handleSave(values, currentVariable?.id);
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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveVariable)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>{currentVariable?.id ? 'Edit' : 'New'} Variable</DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

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
                            name="value"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Value</FormLabel>

                                    <FormControl>
                                        <Textarea {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <p className="text-sm text-content-neutral-secondary">
                            {`Reference it as \${vars.${watchedName || 'NAME'}}`}
                        </p>

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

export default VariableDialog;
