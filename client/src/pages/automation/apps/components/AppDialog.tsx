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
import Switch from '@/components/Switch/Switch';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {deleteAppIcon, uploadAppIcon} from '@/pages/automation/apps/appIcon';
import AppIconField from '@/pages/automation/apps/components/AppIconField';
import AppSecretKeyView from '@/pages/automation/apps/components/AppSecretKeyView';
import AppUserAuthModeSelect from '@/pages/automation/apps/components/AppUserAuthModeSelect';
import {APP_MANAGE_SCOPE, HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import useAppUserAuthAvailability from '@/pages/automation/apps/hooks/useAppUserAuthAvailability';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import CategoryFormField from '@/shared/components/entity-form/CategoryFormField';
import ResourceVisibilityPicker, {
    ResourceVisibilityValueType,
} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {
    App,
    AppCategoryInput,
    AppHostingTarget,
    AppUserAuthMode,
    ResourceVisibility,
    useAppCategoriesQuery,
    useCreateAppMutation,
    useMyWorkspaceScopesQuery,
    useSetAppVisibilityMutation,
    useUpdateAppMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    category: z
        .object({
            id: z.string().optional(),
            label: z.string().optional(),
            name: z.string(),
            value: z.string().optional(),
        })
        .optional(),
    description: z.string().max(1024, 'Description cannot be longer than 1024 characters').optional(),
    hostingTarget: z.enum(AppHostingTarget),
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
    portalDiscoverable: z.boolean(),
    userAuthMode: z.enum(AppUserAuthMode),
});

type AppFormType = z.infer<typeof formSchema>;

type AppDialogAppType = Pick<App, 'description' | 'id' | 'name'> &
    Partial<Pick<App, 'categoryId' | 'hasIcon' | 'portalDiscoverable' | 'visibility' | 'workspaceId'>>;

interface AppCategoryI {
    id: string;
    name: string;
}

const getCategoryValue = (app?: AppDialogAppType, categories?: AppCategoryI[]): AppFormType['category'] => {
    const category = app?.categoryId ? categories?.find((appCategory) => appCategory.id === app.categoryId) : undefined;

    return category ? {id: category.id, label: category.name, name: category.name} : undefined;
};

const getFormValues = (
    app?: AppDialogAppType,
    categories?: AppCategoryI[],
    userAuthMode: AppUserAuthMode = AppUserAuthMode.Required
): AppFormType => ({
    category: getCategoryValue(app, categories),
    description: app?.description || '',
    hostingTarget: AppHostingTarget.Vercel,
    name: app?.name || '',
    portalDiscoverable: !!app?.portalDiscoverable,
    userAuthMode,
});

const toCategoryInput = (
    category: AppFormType['category'],
    currentCategoryId?: string | null
): AppCategoryInput | undefined => {
    if (!category?.name) {
        return undefined;
    }

    if (category.id) {
        return category.id === currentCategoryId ? undefined : {id: category.id};
    }

    return {name: category.name};
};

const getVisibility = (app?: AppDialogAppType): ResourceVisibilityValueType =>
    (app?.visibility || 'WORKSPACE') as ResourceVisibilityValueType;

interface AppDialogProps {
    app?: AppDialogAppType;
    canShare?: boolean;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AppDialog = ({app, canShare = false, onOpenChange, open, triggerNode}: AppDialogProps) => {
    const [iconError, setIconError] = useState<string | undefined>();
    const [iconSaving, setIconSaving] = useState(false);
    const [isOpen, setIsOpen] = useState(!triggerNode && open === undefined);
    const [pendingIconFile, setPendingIconFile] = useState<File | undefined>();
    const [removeIcon, setRemoveIcon] = useState(false);
    const [secretKey, setSecretKey] = useState<string | undefined>();
    const [visibility, setVisibility] = useState<ResourceVisibilityValueType>(getVisibility(app));

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const dialogOpen = open ?? isOpen;

    const categoryWorkspaceId = app?.workspaceId || (currentWorkspaceId ? String(currentWorkspaceId) : '');

    const queryClient = useQueryClient();

    const {data: scopesData} = useMyWorkspaceScopesQuery(
        {workspaceId: categoryWorkspaceId},
        {enabled: dialogOpen && !!categoryWorkspaceId}
    );

    const canManageApps = (scopesData?.myWorkspaceScopes || []).includes(APP_MANAGE_SCOPE);

    const {data: appCategoriesData, isLoading: appCategoriesLoading} = useAppCategoriesQuery(
        {workspaceId: categoryWorkspaceId},
        {enabled: dialogOpen && !!categoryWorkspaceId && canManageApps}
    );

    const {defaultUserAuthMode, userAuthAvailabilityLoading, userAuthAvailable} = useAppUserAuthAvailability(
        categoryWorkspaceId,
        dialogOpen && !app?.id
    );

    const form = useForm<AppFormType>({
        defaultValues: getFormValues(app, undefined, defaultUserAuthMode),
        resolver: zodResolver(formSchema),
    });

    const {control, getFieldState, handleSubmit, reset, setValue} = form;

    const createAppMutation = useCreateAppMutation({
        onSuccess: (data, variables) => {
            setSecretKey(data.createApp.secretKey);

            if (variables?.input.category?.name) {
                queryClient.invalidateQueries({queryKey: ['appCategories']});
            }

            if (pendingIconFile) {
                const createdAppId = data.createApp.appInstance.app.id;

                void saveIconChange(createdAppId).then((errorMessage) => {
                    if (errorMessage) {
                        setIconError(
                            `The app was created, but its icon was not uploaded: ${errorMessage} Edit the app to upload it again.`
                        );
                    }
                });
            }
        },
    });

    const setAppVisibilityMutation = useSetAppVisibilityMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});
        },
    });

    const updateAppMutation = useUpdateAppMutation({
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});

            if (variables?.input.category?.name) {
                queryClient.invalidateQueries({queryKey: ['appCategories']});
            }

            if (app?.id && (pendingIconFile || removeIcon)) {
                void saveIconChange(app.id).then((errorMessage) => {
                    if (errorMessage) {
                        setIconError(`The app was saved, but its icon was not changed: ${errorMessage}`);
                    } else {
                        closeDialog();
                    }
                });
            } else {
                closeDialog();
            }
        },
    });

    const appCategories = useMemo(
        () =>
            appCategoriesData?.appCategories.flatMap((appCategory): AppCategoryI[] =>
                appCategory.id && appCategory.name ? [{id: appCategory.id, name: appCategory.name}] : []
            ),
        [appCategoriesData?.appCategories]
    );

    const showVisibility = !!app?.id && canShare && !!app.workspaceId;

    async function saveIconChange(appId: string): Promise<string | undefined> {
        setIconError(undefined);
        setIconSaving(true);

        try {
            if (pendingIconFile) {
                await uploadAppIcon(appId, pendingIconFile);
            } else if (removeIcon) {
                await deleteAppIcon(appId);
            }

            setPendingIconFile(undefined);
            setRemoveIcon(false);

            return undefined;
        } catch (error) {
            return error instanceof Error ? error.message : 'The icon request failed.';
        } finally {
            setIconSaving(false);

            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});
        }
    }

    function closeDialog() {
        if (secretKey) {
            queryClient.invalidateQueries({queryKey: ['apps']});
        }

        reset(getFormValues(app, appCategories, defaultUserAuthMode));

        setIconError(undefined);
        setPendingIconFile(undefined);
        setRemoveIcon(false);
        setSecretKey(undefined);
        setVisibility(getVisibility(app));
        setIsOpen(false);

        onOpenChange?.(false);
    }

    function saveApp(values: AppFormType) {
        if (app?.id) {
            if (showVisibility && app.workspaceId && visibility !== getVisibility(app)) {
                setAppVisibilityMutation.mutate({
                    appId: app.id,
                    visibility: visibility as ResourceVisibility,
                    workspaceId: app.workspaceId,
                });
            }

            const category = toCategoryInput(values.category, app.categoryId);

            updateAppMutation.mutate({
                id: app.id,
                input: {
                    ...(category ? {category} : {}),
                    description: values.description ?? '',
                    name: values.name,
                    portalDiscoverable: values.portalDiscoverable,
                },
            });
        } else {
            const category = toCategoryInput(values.category);

            createAppMutation.mutate({
                environmentId: String(currentEnvironmentId),
                input: {
                    ...(category ? {category} : {}),
                    description: values.description || undefined,
                    hostingTarget: values.hostingTarget,
                    name: values.name,
                    portalDiscoverable: values.portalDiscoverable,
                    userAuthMode: values.userAuthMode,
                },
                workspaceId: String(currentWorkspaceId),
            });
        }
    }

    useEffect(() => {
        if (!appCategories || getFieldState('category').isDirty) {
            return;
        }

        setValue('category', getCategoryValue(app, appCategories));
    }, [app, appCategories, getFieldState, setValue]);

    useEffect(() => {
        if (app?.id || userAuthAvailable === undefined || getFieldState('userAuthMode').isDirty) {
            return;
        }

        setValue('userAuthMode', defaultUserAuthMode);
    }, [app?.id, defaultUserAuthMode, getFieldState, setValue, userAuthAvailable]);

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (nextOpen) {
                    reset(getFormValues(app, appCategories, defaultUserAuthMode));

                    setVisibility(getVisibility(app));
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

                                        {!app?.id && (
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
                                                                {Object.values(AppHostingTarget).map(
                                                                    (hostingTarget) => (
                                                                        <SelectItem
                                                                            key={hostingTarget}
                                                                            value={hostingTarget}
                                                                        >
                                                                            {HOSTING_TARGET_LABELS[hostingTarget]}
                                                                        </SelectItem>
                                                                    )
                                                                )}
                                                            </SelectContent>
                                                        </Select>

                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        )}

                                        {!app?.id && (
                                            <FormField
                                                control={control}
                                                name="userAuthMode"
                                                render={({field}) => (
                                                    <FormItem>
                                                        <FormLabel>Sign-in</FormLabel>

                                                        <AppUserAuthModeSelect
                                                            onValueChange={field.onChange}
                                                            userAuthAvailable={userAuthAvailable}
                                                            value={field.value}
                                                        />

                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        )}

                                        <CategoryFormField
                                            categories={appCategories}
                                            categoriesLoading={appCategoriesLoading}
                                        />

                                        <FormField
                                            control={control}
                                            name="portalDiscoverable"
                                            render={({field}) => (
                                                <FormItem>
                                                    <Switch
                                                        checked={field.value}
                                                        description="An app appears in the App Portal only when it is ACTIVE and has an instance you can open."
                                                        label="Show in App Portal"
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormItem>
                                            )}
                                        />

                                        <AppIconField
                                            appId={app?.id}
                                            hasIcon={!!app?.hasIcon}
                                            onPendingFileChange={setPendingIconFile}
                                            onRemoveChange={setRemoveIcon}
                                            pendingFile={pendingIconFile}
                                            remove={removeIcon}
                                        />

                                        {showVisibility && (
                                            <fieldset className="space-y-2 border-0 p-0">
                                                <legend className="text-sm font-medium">Visibility</legend>

                                                <ResourceVisibilityPicker
                                                    grantedUserIds={[]}
                                                    onGrantedUserIdsChange={() => undefined}
                                                    onVisibilityChange={setVisibility}
                                                    showSpecificPeopleOption={false}
                                                    visibility={visibility}
                                                />
                                            </fieldset>
                                        )}
                                    </fieldset>
                                )}

                                {iconSaving && (
                                    <p className="mt-4 text-sm text-content-neutral-secondary">Saving the icon...</p>
                                )}

                                {iconError && (
                                    <p className="mt-4 text-sm text-content-destructive" role="alert">
                                        {iconError}
                                    </p>
                                )}
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton label={secretKey ? 'Done' : 'Cancel'} />

                                {!secretKey && (
                                    <Button
                                        disabled={
                                            createAppMutation.isPending ||
                                            iconSaving ||
                                            setAppVisibilityMutation.isPending ||
                                            updateAppMutation.isPending ||
                                            userAuthAvailabilityLoading
                                        }
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
