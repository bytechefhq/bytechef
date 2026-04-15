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
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface OrganizationConnectionDialogProps {
    onClose: () => void;
    onSave: (data: {
        componentName: string;
        connectionVersion: number;
        environmentId: number;
        name: string;
        parameters: Record<string, unknown>;
    }) => void;
}

const OrganizationConnectionDialog = ({onClose, onSave}: OrganizationConnectionDialogProps) => {
    const [componentName, setComponentName] = useState('');
    const [connectionVersion, setConnectionVersion] = useState('1');
    const [environmentId, setEnvironmentId] = useState('');
    const [name, setName] = useState('');
    const [parametersJson, setParametersJson] = useState('{}');
    const [parametersError, setParametersError] = useState('');
    const [formError, setFormError] = useState('');

    const environments = useEnvironmentStore(useShallow((state) => state.environments));

    const handleSave = () => {
        let parsedParameters: Record<string, unknown>;

        try {
            parsedParameters = JSON.parse(parametersJson);
        } catch {
            setParametersError('Invalid JSON');

            return;
        }

        setParametersError('');

        const parsedVersion = parseInt(connectionVersion, 10);
        const parsedEnvironmentId = parseInt(environmentId, 10);

        if (isNaN(parsedVersion) || isNaN(parsedEnvironmentId)) {
            // Unreachable via the Save button because isSaveDisabled already blocks empty values,
            // but fail loudly at the form level rather than attach the error to the Parameters
            // field where it would mislead the user into editing the wrong input.
            setFormError('Invalid version or environment selection');

            return;
        }

        setFormError('');

        onSave({
            componentName,
            connectionVersion: parsedVersion,
            environmentId: parsedEnvironmentId,
            name,
            parameters: parsedParameters,
        });
    };

    const isSaveDisabled = !name.trim() || !componentName.trim() || !environmentId || !connectionVersion;

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent>
                <div className="flex flex-col gap-4">
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <div className="flex flex-col space-y-1">
                            <DialogTitle>New Organization Connection</DialogTitle>

                            <DialogDescription>Create a shared connection for the organization.</DialogDescription>
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    <fieldset className="space-y-4 border-0">
                        <div className="space-y-2">
                            <Label htmlFor="conn-name">Name</Label>

                            <Input
                                id="conn-name"
                                onChange={(event) => setName(event.target.value)}
                                placeholder="My Connection"
                                value={name}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="conn-component">Component Name</Label>

                            <Input
                                id="conn-component"
                                onChange={(event) => setComponentName(event.target.value)}
                                placeholder="e.g. github"
                                value={componentName}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="conn-version">Connection Version</Label>

                            <Input
                                id="conn-version"
                                min={1}
                                onChange={(event) => setConnectionVersion(event.target.value)}
                                type="number"
                                value={connectionVersion}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="conn-environment">Environment</Label>

                            <Select onValueChange={setEnvironmentId} value={environmentId}>
                                <SelectTrigger id="conn-environment">
                                    <SelectValue placeholder="Select environment" />
                                </SelectTrigger>

                                <SelectContent>
                                    {environments.map((environment) => (
                                        <SelectItem key={environment.id} value={String(environment.id)}>
                                            {environment.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="conn-parameters">Parameters (JSON)</Label>

                            <Textarea
                                id="conn-parameters"
                                onChange={(event) => {
                                    setParametersJson(event.target.value);
                                    setParametersError('');
                                }}
                                placeholder="{}"
                                rows={4}
                                value={parametersJson}
                            />

                            {parametersError && <p className="text-sm text-destructive">{parametersError}</p>}
                        </div>

                        {formError && <p className="text-sm text-destructive">{formError}</p>}
                    </fieldset>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button onClick={onClose} variant="outline">
                                Cancel
                            </Button>
                        </DialogClose>

                        <Button disabled={isSaveDisabled} onClick={handleSave}>
                            Save
                        </Button>
                    </DialogFooter>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default OrganizationConnectionDialog;
