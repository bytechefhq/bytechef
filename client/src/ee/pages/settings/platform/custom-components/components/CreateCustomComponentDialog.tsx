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
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {CustomComponentLanguage, useCreateCustomComponentMutation} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    language: z.enum(CustomComponentLanguage),
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
});

interface CreateCustomComponentDialogProps {
    trigger?: ReactNode;
}

const CreateCustomComponentDialog = ({trigger}: CreateCustomComponentDialogProps) => {
    const [open, setOpen] = useState(false);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            language: CustomComponentLanguage.Javascript,
            name: '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset} = form;

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const createCustomComponentMutation = useCreateCustomComponentMutation({
        onSuccess: (data) => {
            queryClient.invalidateQueries({queryKey: ['customComponents']});

            setOpen(false);
            reset();

            navigate(String(data.createCustomComponent.id));
        },
    });

    const handleOpenChange = (newOpen: boolean) => {
        setOpen(newOpen);

        if (!newOpen) {
            reset();
        }
    };

    const handleCreateCustomComponent = () => {
        const {language, name} = getValues();

        createCustomComponentMutation.mutate({language, name});
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>
                {trigger ?? (
                    <Button variant="outline">
                        <PlusIcon className="mr-2 size-4" />
                        New Component
                    </Button>
                )}
            </DialogTrigger>

            <DialogContent>
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleCreateCustomComponent)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>New Custom Component</DialogTitle>

                                <DialogDescription>
                                    Create an empty custom component and start building it.
                                </DialogDescription>
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
                                        <Input {...field} />
                                    </FormControl>

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
                                            <SelectItem value={CustomComponentLanguage.Javascript}>
                                                JavaScript
                                            </SelectItem>
                                        </SelectContent>
                                    </Select>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <Button onClick={() => setOpen(false)} type="button" variant="ghost">
                                Cancel
                            </Button>

                            <Button disabled={createCustomComponentMutation.isPending} type="submit">
                                Create
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default CreateCustomComponentDialog;
