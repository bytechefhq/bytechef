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
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {
    AppInstance,
    useAppInstanceConfigurationQuery,
    useUpdateAppInstanceConfigurationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {FormEvent, ReactNode, useEffect, useState} from 'react';

const MAX_CONFIGURATION_BYTES = 64 * 1024;

const parseConfiguration = (configurationText: string): {configuration?: object; error?: string} => {
    let parsedConfiguration: unknown;

    try {
        parsedConfiguration = JSON.parse(configurationText);
    } catch {
        return {error: 'Configuration must be valid JSON'};
    }

    if (parsedConfiguration === null || typeof parsedConfiguration !== 'object' || Array.isArray(parsedConfiguration)) {
        return {error: 'Configuration must be a JSON object'};
    }

    const serializedConfiguration = JSON.stringify(parsedConfiguration);

    if (new TextEncoder().encode(serializedConfiguration).length > MAX_CONFIGURATION_BYTES) {
        return {error: 'Configuration must be at most 64 KB'};
    }

    return {configuration: parsedConfiguration};
};

interface AppInstanceConfigurationDialogProps {
    appInstance: Pick<AppInstance, 'id' | 'name'>;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AppInstanceConfigurationDialog = ({
    appInstance,
    onOpenChange,
    open,
    triggerNode,
}: AppInstanceConfigurationDialogProps) => {
    const [configurationText, setConfigurationText] = useState('');
    const [errorMessage, setErrorMessage] = useState<string | undefined>();
    const [isOpen, setIsOpen] = useState(false);

    const queryClient = useQueryClient();

    const dialogOpen = open ?? isOpen;

    const {data: appInstanceConfigurationData} = useAppInstanceConfigurationQuery(
        {id: appInstance.id},
        {enabled: dialogOpen}
    );

    const updateAppInstanceConfigurationMutation = useUpdateAppInstanceConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appInstanceConfiguration']});

            handleOpenChange(false);
        },
    });

    const storedConfiguration = appInstanceConfigurationData?.appInstance.configuration;

    function handleOpenChange(nextOpen: boolean) {
        setErrorMessage(undefined);
        setIsOpen(nextOpen);

        onOpenChange?.(nextOpen);
    }

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        const {configuration, error} = parseConfiguration(configurationText);

        if (error || !configuration) {
            setErrorMessage(error);

            return;
        }

        setErrorMessage(undefined);

        updateAppInstanceConfigurationMutation.mutate({configuration, id: appInstance.id});
    };

    useEffect(() => {
        if (dialogOpen && storedConfiguration !== undefined) {
            setConfigurationText(JSON.stringify(storedConfiguration || {}, null, 4));
        }
    }, [dialogOpen, storedConfiguration]);

    return (
        <Dialog onOpenChange={handleOpenChange} open={dialogOpen}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent size="md">
                <DialogMain>
                    <DialogHeader
                        description="A JSON object of at most 64 KB, visible to everyone who can view this instance, so it must not hold secrets (credentials belong in connections)."
                        title={`Configure ${appInstance.name}`}
                    />

                    <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
                        <DialogBody>
                            <fieldset className="space-y-2 border-0 p-0">
                                <Label htmlFor="app-instance-configuration">Configuration (JSON)</Label>

                                <Textarea
                                    aria-invalid={errorMessage !== undefined}
                                    className="min-h-60 font-mono text-sm"
                                    id="app-instance-configuration"
                                    onChange={(event) => setConfigurationText(event.target.value)}
                                    spellCheck={false}
                                    value={configurationText}
                                />

                                {errorMessage && <p className="text-sm text-content-destructive">{errorMessage}</p>}
                            </fieldset>
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button
                                disabled={updateAppInstanceConfigurationMutation.isPending}
                                label="Save"
                                type="submit"
                            />
                        </DialogFooter>
                    </form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AppInstanceConfigurationDialog;
