import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import useDataTableListItemDropdownMenu from '@/pages/automation/datatables/components/hooks/useDataTableListItemDropdownMenu';
import EEVersion from '@/shared/edition/EEVersion';
import {PromotionResourceType, useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {CopyIcon, DownloadIcon, EditIcon, EllipsisVerticalIcon, RocketIcon, Trash2Icon} from 'lucide-react';
import {Suspense, lazy, useState} from 'react';

const EnvironmentPromotionDialog = lazy(
    () => import('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog')
);

interface DataTableListItemDropdownMenuProps {
    baseName: string;
    dataTableId: string;
}

const DataTableListItemDropdownMenu = ({baseName, dataTableId}: DataTableListItemDropdownMenuProps) => {
    const [showPromotionDialog, setShowPromotionDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {handleDeleteClick, handleDuplicateClick, handleExportCsvClick, handleRenameClick} =
        useDataTableListItemDropdownMenu({
            baseName,
            dataTableId,
        });

    const queryClient = useQueryClient();

    const environmentsQuery = useEnvironmentsQuery();

    const showPromoteToEnvironment = (environmentsQuery.data?.environments?.length ?? 0) >= 2;

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button aria-label="Table menu" icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem icon={<EditIcon />} label="Rename" onClick={handleRenameClick} />

                    <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onClick={handleDuplicateClick} />

                    <DropdownMenuItem icon={<DownloadIcon />} label="Export CSV" onClick={handleExportCsvClick} />

                    {showPromoteToEnvironment && (
                        <EEVersion hidden={true}>
                            <DropdownMenuItem
                                icon={<RocketIcon />}
                                label="Promote to environment…"
                                onClick={() => setShowPromotionDialog(true)}
                            />
                        </EEVersion>
                    )}

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={handleDeleteClick}
                        variant="destructive"
                    />
                </DropdownMenuContent>
            </DropdownMenu>

            {showPromotionDialog && (
                <EEVersion hidden={true}>
                    <Suspense fallback={null}>
                        <EnvironmentPromotionDialog
                            onClose={() => setShowPromotionDialog(false)}
                            onPromoted={() => {
                                void queryClient.invalidateQueries({queryKey: ['dataTables']});
                                void queryClient.invalidateQueries({queryKey: ['DataTableStorageUsage']});
                            }}
                            resourceType={PromotionResourceType.DataTable}
                            sourceEnvironmentId={Number(currentEnvironmentId)}
                            sourceId={dataTableId}
                            sourceName={baseName}
                            targetListPath="/automation/datatables"
                        />
                    </Suspense>
                </EEVersion>
            )}
        </>
    );
};

export default DataTableListItemDropdownMenu;
