import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Input} from '@/components/ui/input';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {ChangeEvent, useState} from 'react';

interface ImportCodeWorkflowDialogProps {
    deployUrl: string;
    description: string;
    fileFieldName: string;
    formFields?: Record<string, string>;
    onClose: () => void;
    onImported?: () => void;
    title: string;
}

const ImportCodeWorkflowDialog = ({
    deployUrl,
    description,
    fileFieldName,
    formFields,
    onClose,
    onImported,
    title,
}: ImportCodeWorkflowDialogProps) => {
    const [error, setError] = useState<string | null>(null);
    const [file, setFile] = useState<File | null>(null);
    const [importing, setImporting] = useState(false);
    const [warnings, setWarnings] = useState<string[]>([]);

    const javaEnabled = useApplicationInfoStore((state) => state.workflow.codeWorkflow.javaEnabled);

    const acceptedExtensions = javaEnabled ? '.jar,.js,.py' : '.js,.py';

    const acceptedExtensionsLabel = acceptedExtensions
        .split(',')
        .map((extension) => extension.replace('.', '').toUpperCase())
        .join(', ');

    const imported = warnings.length > 0;

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        const selectedFile = event.target.files?.[0] || null;

        if (selectedFile && !javaEnabled && selectedFile.name.toLowerCase().endsWith('.jar')) {
            setError('Java code workflows are disabled on this instance.');
            setFile(null);

            return;
        }

        setError(null);
        setFile(selectedFile);
    };

    const handleImport = async () => {
        if (!file) {
            return;
        }

        setImporting(true);
        setError(null);

        const formData = new FormData();

        Object.entries(formFields || {}).forEach(([name, value]) => formData.append(name, value));

        formData.append(fileFieldName, file);

        try {
            const response = await fetch(deployUrl, {
                body: formData,
                headers: {
                    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                },
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Import failed: ${response.statusText}`);
            }

            const result: {warnings?: string[]} = response.status === 204 ? {} : await response.json();

            onImported?.();

            if (result.warnings && result.warnings.length > 0) {
                setWarnings(result.warnings);
            } else {
                onClose();
            }
        } catch (importError) {
            setError(importError instanceof Error ? importError.message : 'Import failed');
        } finally {
            setImporting(false);
        }
    };

    return (
        <Dialog onOpenChange={(open) => !open && !importing && onClose()} open>
            <DialogContent onInteractOutside={(event) => importing && event.preventDefault()}>
                <DialogMain>
                    <DialogHeader description={description} title={title} />

                    <DialogBody>
                        <fieldset className="flex flex-col gap-1 border-0">
                            <label className="text-sm" htmlFor="import-code-workflow-file">
                                Code file
                            </label>

                            <Input
                                accept={acceptedExtensions}
                                disabled={importing || imported}
                                id="import-code-workflow-file"
                                onChange={handleFileChange}
                                type="file"
                            />

                            <p className="text-xs text-content-neutral-tertiary">{acceptedExtensionsLabel} files</p>
                        </fieldset>

                        {warnings.length > 0 && (
                            <ul className="space-y-1 text-sm text-content-warning">
                                {warnings.map((warning) => (
                                    <li key={warning}>{warning}</li>
                                ))}
                            </ul>
                        )}

                        {error && <p className="text-sm text-destructive">{error}</p>}
                    </DialogBody>

                    <DialogFooter>
                        {imported ? (
                            <Button label="Done" onClick={onClose} />
                        ) : (
                            <>
                                <Button disabled={importing} label="Cancel" onClick={onClose} variant="ghost" />

                                <Button
                                    disabled={!file || importing}
                                    label={importing ? 'Importing...' : 'Import'}
                                    onClick={handleImport}
                                />
                            </>
                        )}
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ImportCodeWorkflowDialog;
