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
import AppSecretKeyView from '@/pages/automation/apps/components/AppSecretKeyView';
import AppUserAuthModeSelect from '@/pages/automation/apps/components/AppUserAuthModeSelect';
import {APP_ENVIRONMENT_IDS, HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import useAppManageScopes from '@/pages/automation/apps/hooks/useAppManageScopes';
import useAppUserAuthAvailability from '@/pages/automation/apps/hooks/useAppUserAuthAvailability';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {toEnvironmentName} from '@/shared/constants';
import {
    AppHostingTarget,
    AppInstance,
    AppUserAuthMode,
    useCreateAppInstanceMutation,
    useUpdateAppInstanceMutation,
    useUpdateAppInstanceUserAuthMutation,
} from '@/shared/middleware/graphql';
import {useGetUserWorkspacesQuery} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useMemo, useState} from 'react';
import {useForm, useWatch} from 'react-hook-form';
import {z} from 'zod';

const HTTP_PROTOCOL_PATTERN = /^https?$/;

const formSchema = z.object({
    environmentId: z.string().min(1, 'Environment is required'),
    hostingTarget: z.enum(AppHostingTarget),
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
    url: z
        .string()
        .optional()
        .refine(
            (value) => !value || z.url({protocol: HTTP_PROTOCOL_PATTERN}).safeParse(value).success,
            'URL must be an http or https URL'
        ),
    userAuthMode: z.enum(AppUserAuthMode),
    workspaceId: z.string(),
});

type AppInstanceFormType = z.infer<typeof formSchema>;

type AppInstanceDialogInstanceType = Pick<
    AppInstance,
    'environmentId' | 'hostingTarget' | 'id' | 'name' | 'url' | 'userAuthMode' | 'workspaceId'
>;

interface AppInstanceDialogProps {
    appId: string;
    appInstance?: AppInstanceDialogInstanceType;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AppInstanceDialog = ({appId, appInstance, onOpenChange, open, triggerNode}: AppInstanceDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode && open === undefined);
    const [secretKey, setSecretKey] = useState<string | undefined>();

    const account = useAuthenticationStore((state) => state.account);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const dialogOpen = open ?? isOpen;

    const getFormValues = (userAuthMode: AppUserAuthMode = AppUserAuthMode.Required): AppInstanceFormType => ({
        environmentId: appInstance?.environmentId ?? String(currentEnvironmentId),
        hostingTarget: appInstance?.hostingTarget || AppHostingTarget.Vercel,
        name: appInstance?.name || '',
        url: appInstance?.url || '',
        userAuthMode: appInstance?.userAuthMode || userAuthMode,
        workspaceId: appInstance?.workspaceId ?? String(currentWorkspaceId),
    });

    const form = useForm<AppInstanceFormType>({
        defaultValues: getFormValues(),
        resolver: zodResolver(formSchema),
    });

    const {control, getFieldState, getValues, handleSubmit, reset, setValue} = form;

    const {data: userWorkspaces} = useGetUserWorkspacesQuery(
        account?.id ?? 0,
        dialogOpen && !appInstance?.id && account?.id != null
    );

    const selectedEnvironmentId = useWatch({control, name: 'environmentId'});
    const selectedWorkspaceId = useWatch({control, name: 'workspaceId'});

    const {defaultUserAuthMode, userAuthAvailabilityLoading, userAuthAvailable} = useAppUserAuthAvailability(
        selectedWorkspaceId ?? '',
        dialogOpen && !appInstance?.id
    );

    const workspaceTargets = useMemo(
        () =>
            (userWorkspaces ?? [])
                .filter((workspace) => workspace.id != null)
                .map((workspace) => ({environmentId: selectedEnvironmentId, workspaceId: String(workspace.id)})),
        [selectedEnvironmentId, userWorkspaces]
    );

    const canManageWorkspace = useAppManageScopes(appInstance?.id ? [] : workspaceTargets);

    const manageableWorkspaces = useMemo(
        () =>
            (userWorkspaces ?? []).filter(
                (workspace) => workspace.id != null && canManageWorkspace(String(workspace.id), selectedEnvironmentId)
            ),
        [canManageWorkspace, selectedEnvironmentId, userWorkspaces]
    );

    const workspaceOffered = manageableWorkspaces.some((workspace) => String(workspace.id) === selectedWorkspaceId);

    const createAppInstanceMutation = useCreateAppInstanceMutation({
        onSuccess: (data) => {
            queryClient.invalidateQueries({queryKey: ['app']});
            queryClient.invalidateQueries({queryKey: ['apps']});

            setSecretKey(data.createAppInstance.secretKey);
        },
    });

    const updateAppInstanceUserAuthMutation = useUpdateAppInstanceUserAuthMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['app']});
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['hostedAppInstances']});

            closeDialog();
        },
    });

    const updateAppInstanceMutation = useUpdateAppInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['app']});
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['hostedAppInstances']});

            const userAuthMode = getValues('userAuthMode');

            if (appInstance?.id && userAuthMode !== appInstance.userAuthMode) {
                updateAppInstanceUserAuthMutation.mutate({id: appInstance.id, userAuthMode});

                return;
            }

            closeDialog();
        },
    });

    function closeDialog() {
        reset(getFormValues(defaultUserAuthMode));

        setSecretKey(undefined);
        setIsOpen(false);

        onOpenChange?.(false);
    }

    function saveAppInstance(values: AppInstanceFormType) {
        if (appInstance?.id) {
            updateAppInstanceMutation.mutate({
                id: appInstance.id,
                input: {
                    hostingTarget: values.hostingTarget,
                    name: values.name,
                    url: values.url ?? '',
                },
            });

            return;
        }

        if (!workspaceOffered) {
            form.setError('workspaceId', {message: 'Workspace is required'});

            return;
        }

        createAppInstanceMutation.mutate({
            appId,
            environmentId: values.environmentId,
            input: {
                hostingTarget: values.hostingTarget,
                name: values.name,
                userAuthMode: values.userAuthMode,
            },
            workspaceId: values.workspaceId,
        });
    }

    useEffect(() => {
        if (appInstance?.id || userAuthAvailable === undefined || getFieldState('userAuthMode').isDirty) {
            return;
        }

        setValue('userAuthMode', defaultUserAuthMode);
    }, [appInstance?.id, defaultUserAuthMode, getFieldState, setValue, userAuthAvailable]);

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (nextOpen) {
                    reset(getFormValues(defaultUserAuthMode));

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
                    <DialogHeader
                        title={
                            secretKey ? 'Save your instance key' : appInstance?.id ? 'Edit Instance' : 'New Instance'
                        }
                    />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveAppInstance)}>
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

                                        {!appInstance?.id && (
                                            <FormField
                                                control={control}
                                                name="environmentId"
                                                render={({field}) => (
                                                    <FormItem>
                                                        <FormLabel>Environment</FormLabel>

                                                        <Select
                                                            onValueChange={(environmentId) => {
                                                                field.onChange(environmentId);

                                                                setValue('workspaceId', String(currentWorkspaceId));
                                                            }}
                                                            value={field.value}
                                                        >
                                                            <FormControl>
                                                                <SelectTrigger>
                                                                    <SelectValue />
                                                                </SelectTrigger>
                                                            </FormControl>

                                                            <SelectContent>
                                                                {APP_ENVIRONMENT_IDS.map((environmentId) => (
                                                                    <SelectItem
                                                                        key={environmentId}
                                                                        value={String(environmentId)}
                                                                    >
                                                                        {toEnvironmentName(environmentId)}
                                                                    </SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>

                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        )}

                                        {!appInstance?.id && (
                                            <FormField
                                                control={control}
                                                name="workspaceId"
                                                render={({field}) => (
                                                    <FormItem>
                                                        <FormLabel>Workspace</FormLabel>

                                                        <Select
                                                            onValueChange={field.onChange}
                                                            value={workspaceOffered ? field.value : ''}
                                                        >
                                                            <FormControl>
                                                                <SelectTrigger>
                                                                    <SelectValue placeholder="Choose a workspace" />
                                                                </SelectTrigger>
                                                            </FormControl>

                                                            <SelectContent>
                                                                {manageableWorkspaces.map((workspace) => (
                                                                    <SelectItem
                                                                        key={workspace.id}
                                                                        value={String(workspace.id)}
                                                                    >
                                                                        {workspace.name}
                                                                    </SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>

                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        )}

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
                                            name="userAuthMode"
                                            render={({field}) => (
                                                <FormItem>
                                                    <FormLabel>Sign-in</FormLabel>

                                                    <AppUserAuthModeSelect
                                                        onValueChange={field.onChange}
                                                        userAuthAvailable={
                                                            appInstance?.id ? undefined : userAuthAvailable
                                                        }
                                                        value={field.value}
                                                    />

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        {appInstance?.id && (
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
                                        )}
                                    </fieldset>
                                )}
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton label={secretKey ? 'Done' : 'Cancel'} />

                                {!secretKey && (
                                    <Button
                                        disabled={
                                            createAppInstanceMutation.isPending ||
                                            updateAppInstanceMutation.isPending ||
                                            updateAppInstanceUserAuthMutation.isPending ||
                                            userAuthAvailabilityLoading
                                        }
                                        label={appInstance?.id ? 'Save' : 'Create Instance'}
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

export default AppInstanceDialog;
