import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {type ConnectedUserImportResult} from '@/ee/shared/middleware/embedded/connected-user';
import {useImportConnectedUsersMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const ConnectedUserImportDialog = ({onClose}: {onClose: () => void}) => {
    const [file, setFile] = useState<File>();
    const [importResult, setImportResult] = useState<ConnectedUserImportResult>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const importConnectedUsersMutation = useImportConnectedUsersMutation({
        onSuccess: (result) => {
            setImportResult(result);

            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUsers});
        },
    });

    const handleImport = () => {
        if (!file) {
            return;
        }

        importConnectedUsersMutation.mutate({environmentId: currentEnvironmentId, file});
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>Import connected users</DialogTitle>
                </DialogHeader>

                <div className="flex flex-col gap-4">
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="connected-user-import-file">CSV file</Label>

                        <input
                            accept=".csv"
                            className="text-sm"
                            id="connected-user-import-file"
                            onChange={(event) => {
                                setFile(event.target.files?.[0]);
                                setImportResult(undefined);
                            }}
                            type="file"
                        />

                        <p className="text-sm text-muted-foreground">
                            Columns: externalId (required), name, email, plus any metadata columns
                        </p>
                    </div>

                    {importResult && (
                        <div className="flex flex-col gap-2 text-sm">
                            <p className="font-medium">Created {importResult.createdCount ?? 0}</p>

                            {!!importResult.skippedExternalIds?.length && (
                                <div>
                                    <p>Skipped (duplicates):</p>

                                    <ul className="list-disc pl-5">
                                        {[...new Set(importResult.skippedExternalIds)].map((externalId) => (
                                            <li key={externalId}>{externalId}</li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            {!!importResult.rejectedRows?.length && (
                                <div>
                                    <p>Rejected:</p>

                                    <ul className="list-disc pl-5">
                                        {importResult.rejectedRows.map((rejectedRow) => (
                                            <li key={rejectedRow.rowNumber}>
                                                {`Row ${rejectedRow.rowNumber}: ${rejectedRow.reason}`}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </div>
                    )}
                </div>

                <DialogFooter>
                    <Button label="Close" onClick={onClose} type="button" variant="outline" />

                    <Button
                        disabled={!file || importConnectedUsersMutation.isPending}
                        label="Import"
                        onClick={handleImport}
                        type="button"
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserImportDialog;
