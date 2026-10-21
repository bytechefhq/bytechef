import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
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
import Switch from '@/components/Switch/Switch';
import {Checkbox} from '@/components/ui/checkbox';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {
    useAppBindableConnectionsQuery,
    useAppComponentOperationsQuery,
    useBindAppConnectionMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';
import {useForm, useWatch} from 'react-hook-form';
import {z} from 'zod';

const PROXY_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'];

const formSchema = z
    .object({
        actionNames: z.array(z.string()),
        bindingKey: z
            .string()
            .regex(
                /^[a-z][a-z0-9_-]{0,63}$/,
                'Binding key must start with a lowercase letter and contain only lowercase letters, digits, hyphens and underscores (max 64 characters)'
            ),
        connectionId: z.string().min(1, 'Connection is required'),
        proxyAllowedMethods: z.array(z.string()),
        proxyEnabled: z.boolean(),
        toolNames: z.array(z.string()),
    })
    .refine((values) => !values.proxyEnabled || values.proxyAllowedMethods.length > 0, {
        message: 'Select at least one method when the proxy is enabled',
        path: ['proxyAllowedMethods'],
    });

type AppBindingFormType = z.infer<typeof formSchema>;

interface CheckboxGroupProps {
    idPrefix: string;
    names: string[];
    onChange: (names: string[]) => void;
    selectedNames: string[];
}

const CheckboxGroup = ({idPrefix, names, onChange, selectedNames}: CheckboxGroupProps) => (
    <div className="grid grid-cols-2 gap-2">
        {names.map((name) => (
            <div className="flex items-center gap-2" key={name}>
                <Checkbox
                    checked={selectedNames.includes(name)}
                    id={`${idPrefix}-${name}`}
                    onCheckedChange={(checked) =>
                        onChange(checked ? [...selectedNames, name] : selectedNames.filter((item) => item !== name))
                    }
                />

                <Label htmlFor={`${idPrefix}-${name}`}>{name}</Label>
            </div>
        ))}
    </div>
);

interface AppBindingDialogProps {
    appInstanceId: string;
    binding?: AppBindingFormType;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const EMPTY_BINDING: AppBindingFormType = {
    actionNames: [],
    bindingKey: '',
    connectionId: '',
    proxyAllowedMethods: [],
    proxyEnabled: false,
    toolNames: [],
};

const AppBindingDialog = ({appInstanceId, binding, onClose, triggerNode}: AppBindingDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const queryClient = useQueryClient();

    const form = useForm<AppBindingFormType>({
        defaultValues: binding || EMPTY_BINDING,
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset, setValue} = form;

    const [connectionId, proxyEnabled] = useWatch({control, name: ['connectionId', 'proxyEnabled']});

    const {data: bindableConnectionsData} = useAppBindableConnectionsQuery({appInstanceId}, {enabled: isOpen});

    const bindableConnections = useMemo(
        () => bindableConnectionsData?.appBindableConnections ?? [],
        [bindableConnectionsData?.appBindableConnections]
    );

    const selectedConnection = useMemo(
        () => bindableConnections.find((connection) => connection.id === connectionId),
        [bindableConnections, connectionId]
    );

    const {data: operationsData} = useAppComponentOperationsQuery(
        {
            componentName: selectedConnection?.componentName ?? '',
            componentVersion: selectedConnection?.componentVersion ?? 1,
        },
        {enabled: selectedConnection?.componentVersion != null}
    );

    const bindAppConnectionMutation = useBindAppConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appBindings']});

            closeDialog();
        },
    });

    const connectionItems = useMemo(
        () =>
            bindableConnections.map((connection) => ({
                label: `${connection.name} (${connection.componentName})`,
                value: connection.id,
            })),
        [bindableConnections]
    );

    function closeDialog() {
        reset(binding || EMPTY_BINDING);

        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function saveBinding(values: AppBindingFormType) {
        bindAppConnectionMutation.mutate({
            appInstanceId,
            input: {
                actionNames: values.actionNames,
                bindingKey: values.bindingKey,
                connectionId: values.connectionId,
                proxyAllowedMethods: values.proxyEnabled ? values.proxyAllowedMethods : [],
                proxyEnabled: values.proxyEnabled,
                toolNames: values.toolNames,
            },
        });
    }

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (nextOpen) {
                    setIsOpen(true);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent aria-describedby={undefined} size="md">
                <DialogMain>
                    <DialogHeader title={binding ? 'Edit Binding' : 'Bind Connection'} />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveBinding)}>
                            <DialogBody>
                                <fieldset className="space-y-4 border-0 p-0">
                                    <FormField
                                        control={control}
                                        name="bindingKey"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Binding key</FormLabel>

                                                <FormControl>
                                                    <Input disabled={!!binding} placeholder="sales_slack" {...field} />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={control}
                                        name="connectionId"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Connection</FormLabel>

                                                <FormControl>
                                                    <ComboBox
                                                        aria-label="Connection"
                                                        emptyMessage="No connection can be bound to this app."
                                                        items={connectionItems}
                                                        onChange={(item) => {
                                                            const nextConnection = bindableConnections.find(
                                                                (connection) => connection.id === item?.value
                                                            );

                                                            field.onChange(item?.value ?? '');

                                                            if (
                                                                nextConnection?.componentName !==
                                                                selectedConnection?.componentName
                                                            ) {
                                                                setValue('actionNames', []);
                                                                setValue('toolNames', []);
                                                            }
                                                        }}
                                                        value={field.value}
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    {selectedConnection && (
                                        <>
                                            <FormField
                                                control={control}
                                                name="actionNames"
                                                render={({field}) => (
                                                    <FormItem>
                                                        <FormLabel>Allowed actions</FormLabel>

                                                        <CheckboxGroup
                                                            idPrefix="action"
                                                            names={
                                                                operationsData?.appComponentOperations.actionNames ?? []
                                                            }
                                                            onChange={field.onChange}
                                                            selectedNames={field.value}
                                                        />

                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />

                                            <FormField
                                                control={control}
                                                name="toolNames"
                                                render={({field}) => (
                                                    <FormItem>
                                                        <FormLabel>Allowed tools</FormLabel>

                                                        <CheckboxGroup
                                                            idPrefix="tool"
                                                            names={
                                                                operationsData?.appComponentOperations.toolNames ?? []
                                                            }
                                                            onChange={field.onChange}
                                                            selectedNames={field.value}
                                                        />

                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        </>
                                    )}

                                    <FormField
                                        control={control}
                                        name="proxyEnabled"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Proxy</FormLabel>

                                                <Switch
                                                    checked={field.value}
                                                    label="Let the app call the connection API directly"
                                                    onCheckedChange={field.onChange}
                                                />

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    {proxyEnabled && (
                                        <FormField
                                            control={control}
                                            name="proxyAllowedMethods"
                                            render={({field}) => (
                                                <FormItem>
                                                    <FormLabel>Allowed proxy methods</FormLabel>

                                                    <CheckboxGroup
                                                        idPrefix="method"
                                                        names={PROXY_METHODS}
                                                        onChange={field.onChange}
                                                        selectedNames={field.value}
                                                    />

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    )}
                                </fieldset>
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button
                                    disabled={bindAppConnectionMutation.isPending}
                                    label={binding ? 'Save' : 'Bind'}
                                    type="submit"
                                />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AppBindingDialog;
