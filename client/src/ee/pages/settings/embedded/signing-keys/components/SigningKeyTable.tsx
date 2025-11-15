import {Button} from '@/components/ui/button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import SigningKeyDeleteDialog from '@/ee/pages/settings/embedded/signing-keys/components/SigningKeyDeleteDialog';
import SigningKeyDialog from '@/ee/pages/settings/embedded/signing-keys/components/SigningKeyDialog';
import {SigningKey} from '@/ee/shared/middleware/embedded/security';
import {createColumnHelper, flexRender, getCoreRowModel, useReactTable} from '@tanstack/react-table';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon, EditIcon, Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const columnHelper = createColumnHelper<SigningKey>();

interface SigningKeyTableProps {
    signingKeys: SigningKey[];
}

const SigningKeyTable = ({signingKeys}: SigningKeyTableProps) => {
    const [currentSigningKey, setCurrentSigningKey] = useState<SigningKey>();
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const columns = useMemo(
        () => [
            columnHelper.accessor('name', {
                cell: (info) => info.getValue(),
                header: 'Name',
            }),
            columnHelper.accessor('keyId', {
                cell: (info) => (
                    <div className="group flex items-center gap-0.5">
                        <span className="max-w-52 truncate">{info.getValue()}</span>

                        <Button
                            className="invisible group-hover:visible"
                            onClick={() => copyToClipboard(info.getValue())}
                            size="icon"
                            variant="ghost"
                        >
                            <ClipboardIcon aria-hidden="true" className="size-4 text-gray-400" />
                        </Button>
                    </div>
                ),
                header: 'Key Id',
            }),
            columnHelper.accessor('createdDate', {
                cell: (info) => `${info.getValue()?.toLocaleDateString()} ${info.getValue()?.toLocaleTimeString()}`,
                header: 'Created Date',
            }),
            columnHelper.accessor('lastUsedDate', {
                cell: (info) =>
                    `${info.getValue()?.toLocaleDateString() ?? ''} ${info.getValue()?.toLocaleTimeString() ?? ''}`,
                header: 'Last Used Date',
            }),
            columnHelper.accessor('createdBy', {
                cell: (info) => `${info.getValue()}`,
                header: 'Created By',
            }),
            columnHelper.display({
                cell: (info) => (
                    <>
                        <Button
                            onClick={() => {
                                setCurrentSigningKey(info.row.original);
                                setShowEditDialog(true);
                            }}
                            size="icon"
                            variant="ghost"
                        >
                            <EditIcon className="size-4" />
                        </Button>

                        <Button
                            onClick={() => {
                                setCurrentSigningKey(info.row.original);
                                setShowDeleteDialog(true);
                            }}
                            size="icon"
                            variant="ghost"
                        >
                            <Trash2Icon className="h-4 text-destructive" />
                        </Button>
                    </>
                ),
                header: '',
                id: 'actions',
            }),
        ],
        [copyToClipboard]
    );

    const reactTable = useReactTable<SigningKey>({
        columns,
        data: signingKeys,
        getCoreRowModel: getCoreRowModel(),
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    return (
        <div className="w-full space-y-8 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
            <p>
                Use your Signing Keys to sign requests made from the ByteChef SDK. <a href="#">Read our docs</a> for
                more information.
            </p>

            <Table>
                <TableHeader>
                    {headerGroups.map((headerGroup) => (
                        <TableRow className="border-b-border/50" key={headerGroup.id}>
                            {headerGroup.headers.map((header) => (
                                <TableHead
                                    className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"
                                    key={header.id}
                                >
                                    {!header.isPlaceholder &&
                                        flexRender(header.column.columnDef.header, header.getContext())}
                                </TableHead>
                            ))}
                        </TableRow>
                    ))}
                </TableHeader>

                <TableBody>
                    {rows.map((row) => (
                        <TableRow className="cursor-pointer border-b-border/50" key={row.id}>
                            {row.getVisibleCells().map((cell) => (
                                <TableCell
                                    className={twMerge(
                                        'whitespace-nowrap',
                                        cell.id.endsWith('actions') && 'flex justify-end',
                                        cell.id.endsWith('name') && 'truncate xl:min-w-80'
                                    )}
                                    key={cell.id}
                                    onClick={(event) => {
                                        if (cell.id.endsWith('actions')) {
                                            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                            const target = event.target as any;

                                            if (target.getAttribute('data-action') === 'delete') {
                                                console.log(target.getAttribute('data-action'));
                                            } else if (target.getAttribute('data-action') === 'edit') {
                                                console.log(target.getAttribute('data-action'));
                                            }
                                        }
                                    }}
                                >
                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>

            {showDeleteDialog && currentSigningKey && (
                <SigningKeyDeleteDialog apiKeyId={currentSigningKey.id!} onClose={() => setShowDeleteDialog(false)} />
            )}

            {showEditDialog && (
                <SigningKeyDialog onClose={() => setShowEditDialog(false)} signingKey={currentSigningKey} />
            )}
        </div>
    );
};

export default SigningKeyTable;
