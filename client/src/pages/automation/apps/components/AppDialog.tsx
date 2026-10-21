import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import AppSecretKeyView from '@/pages/automation/apps/components/AppSecretKeyView';
import {HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {App, AppHostingTarget, useCreateAppMutation, useUpdateAppMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    description: z.string().max(1024, 'Description cannot be longer than 1024 characters').optional(),
    hostingTarget: z.enum(AppHostingTarget),
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
    url: z
        .string()
        .optional()
        .refine((value) => !value || z.url().safeParse(value).success, 'URL must be a valid URL'),
});

type AppFormType = z.infer<typeof formSchema>;

const getFormValues = (app?: App): AppFormType => ({
    description: app?.description || '',
    hostingTarget: app?.hostingTarget || AppHostingTarget.Vercel,
    name: app?.name || '',
    url: app?.url || '',
});

interface AppDialogProps {
    app?: App;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AppDialog = ({app, onOpenChange, open, triggerNode}: AppDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode && open === undefined);
    const [secretKey, setSecretKey] = useState<string | undefined>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const form = useForm<AppFormType>({
        defaultValues: getFormValues(app),
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const createAppMutation = useCreateAppMutation({
        onSuccess: (data) => {
            setSecretKey(data.createApp.secretKey);
        },
    });

    const updateAppMutation = useUpdateAppMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});

            closeDialog();
        },
    });

    const dialogOpen = open ?? isOpen;

    function closeDialog() {
        if (secretKey) {
            queryClient.invalidateQueries({queryKey: ['apps']});
        }

        reset(getFormValues(app));

        setSecretKey(undefined);
        setIsOpen(false);

        onOpenChange?.(false);
    }

    function saveApp(values: AppFormType) {
        if (app?.id) {
            updateAppMutation.mutate({
                id: app.id,
                input: {
                    description: values.description ?? '',
                    hostingTarget: values.hostingTarget,
                    name: values.name,
                    url: values.url ?? '',
                },
            });
        } else {
            createAppMutation.mutate({
                environmentId: String(currentEnvironmentId),
                input: {
                    description: values.description || undefined,
                    hostingTarget: values.hostingTarget,
                    name: values.name,
                    url: values.url || undefined,
                },
                workspaceId: String(currentWorkspaceId),
            });
        }
    }

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (nextOpen) {
                    reset(getFormValues(app));

                    setIsOpen(true);

                    onOpenChange?.(true);
                } else {
                    closeDialog();
                }
            }}
            open={dialogOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent aria-describedby={undefined} size="md">
                <DialogMain>
                    <DialogHeader title={secretKey ? 'Save your app key' : app?.id ? 'Edit App' : 'New App'} />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveApp)}>
                            <DialogBody>
                                {secretKey ? (
                                    <AppSecretKeyView secretKey={secretKey} />
                                ) : (
                                    <fieldset className="space-y-4 border-0 p-0">
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
                                            name="hostingTarget"
                                            render={({field}) => (
                                                <FormItem>
                                                    <FormLabel>Hosting target</FormLabel>

                                                    <Select onValueChange={field.onChange} value={field.value}>
                                                        <FormControl>
                                                            <SelectTrigger>
                                                                <SelectValue />
                                                            </SelectTrigger>
                                                        </FormControl>

                                                        <SelectContent>
                                                            {Object.values(AppHostingTarget).map((hostingTarget) => (
                                                                <SelectItem key={hostingTarget} value={hostingTarget}>
                                                                    {HOSTING_TARGET_LABELS[hostingTarget]}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={control}
                                            name="url"
                                            render={({field}) => (
                                                <FormItem>
                                                    <FormLabel>URL</FormLabel>

                                                    <FormControl>
                                                        <Input placeholder="https://my-app.vercel.app" {...field} />
                                                    </FormControl>

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    </fieldset>
                                )}
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton label={secretKey ? 'Done' : 'Cancel'} />

                                {!secretKey && (
                                    <Button
                                        disabled={createAppMutation.isPending || updateAppMutation.isPending}
                                        label={app?.id ? 'Save' : 'Create App'}
                                        type="submit"
                                    />
                                )}
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AppDialog;
