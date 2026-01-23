import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {FileText, Loader2, X} from 'lucide-react';
import {useState} from 'react';

import useImportDataTableCsvDialog from '../hooks/useImportDataTableCsvDialog';

const ImportDataTableCsvDialog = () => {
    const [importFile, setImportFile] = useState<File | null>(null);

    const {handleImport, handleOpenChange, isPending, open} = useImportDataTableCsvDialog();

    const handleDialogOpenChange = (nextOpen: boolean) => {
        handleOpenChange(nextOpen);

        if (!nextOpen) {
            setImportFile(null);
        }
    };

    const handleImportClick = async () => {
        if (!importFile) return;

        const text = await importFile.text();

        handleImport(text);
        setImportFile(null);
    };

    const handleCancel = () => {
        handleOpenChange(false);
        setImportFile(null);
    };

    return (
        <Dialog onOpenChange={handleDialogOpenChange} open={open}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Import CSV</DialogTitle>
                </DialogHeader>

                <div className="space-y-3 py-2">
                    <div className="space-y-1">
                        <Label htmlFor="csvFile">CSV file</Label>

                        <Input
                            accept=".csv,text/csv"
                            className="bg-background"
                            id="csvFile"
                            onChange={(event) => {
                                const file = event.target.files?.[0] ?? null;

                                setImportFile(file);
                            }}
                            type="file"
                        />

                        {importFile && (
                            <div className="space-y-2 pt-1">
                                <div className="flex items-center justify-between rounded-md border p-2">
                                    <div className="flex items-center gap-2">
                                        <FileText className="h-4 w-4 text-muted-foreground" />

                                        <span className="text-sm">{importFile.name}</span>

                                        <span className="text-xs text-muted-foreground">
                                            ({(importFile.size / 1024).toFixed(1)} KB)
                                        </span>
                                    </div>

                                    <Button
                                        className="h-6 w-6"
                                        icon={<X className="h-3 w-3" />}
                                        onClick={() => setImportFile(null)}
                                        type="button"
                                        variant="ghost"
                                    />
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                <DialogFooter>
                    <Button onClick={handleCancel} variant="outline">
                        Cancel
                    </Button>

                    <Button
                        disabled={!importFile || isPending}
                        icon={isPending ? <Loader2 className="animate-spin" /> : undefined}
                        onClick={handleImportClick}
                    >
                        {isPending ? 'Importing…' : 'Import'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ImportDataTableCsvDialog;
