import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {getCookie} from '@/shared/util/cookie-utils';
import {useState} from 'react';

interface DeployCodeWorkflowDialogProps {
    onClose: () => void;
    onDeployed?: () => void;
}

const DeployCodeWorkflowDialog = ({onClose, onDeployed}: DeployCodeWorkflowDialogProps) => {
    const [file, setFile] = useState<File | null>(null);
    const [deploying, setDeploying] = useState(false);
    const [warnings, setWarnings] = useState<string[]>([]);
    const [error, setError] = useState<string | null>(null);

    const handleDeploy = async () => {
        if (!file) {
            return;
        }

        setDeploying(true);
        setError(null);

        const formData = new FormData();

        formData.append('projectFile', file);

        try {
            const response = await fetch('/api/embedded/internal/automation/projects/deploy', {
                body: formData,
                headers: {
                    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                },
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Deploy failed: ${response.statusText}`);
            }

            const result: {warnings?: string[]} = await response.json();

            setWarnings(result.warnings || []);

            onDeployed?.();
        } catch (deployError) {
            setError(deployError instanceof Error ? deployError.message : 'Deploy failed');
        } finally {
            setDeploying(false);
        }
    };

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Deploy Code Workflow</DialogTitle>

                        <DialogDescription>
                            Upload a code-native automation project. Redeploying an existing project name updates it in
                            place for every connected user referencing it.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <label className="flex flex-col gap-1 text-sm" htmlFor="deploy-code-workflow-file">
                    Project file
                    <input
                        accept=".jar,.js,.py,.rb"
                        id="deploy-code-workflow-file"
                        onChange={(event) => setFile(event.target.files?.[0] || null)}
                        type="file"
                    />
                </label>

                {warnings.length > 0 && (
                    <ul className="space-y-1 text-sm text-content-warning">
                        {warnings.map((warning) => (
                            <li key={warning}>{warning}</li>
                        ))}
                    </ul>
                )}

                {error && <p className="text-sm text-destructive">{error}</p>}

                <DialogFooter>
                    <Button
                        disabled={!file || deploying}
                        label={deploying ? 'Deploying...' : 'Deploy'}
                        onClick={handleDeploy}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default DeployCodeWorkflowDialog;
