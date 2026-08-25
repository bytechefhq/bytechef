import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useVariablesProvider} from '@/ee/shared/components/variables/providers/variablesProvider';
import {useVariablesStore} from '@/ee/shared/components/variables/stores/useVariablesStore';
import {Variable} from '@/shared/middleware/graphql';
import {coreTableFeatures} from '@/shared/util/table-features';
import {createColumnHelper, flexRender, useTable} from '@tanstack/react-table';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {CopyIcon, EllipsisVerticalIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const VALUE_TRUNCATE_LENGTH = 60;

const columnHelper = createColumnHelper<typeof coreTableFeatures, Variable>();

const toReference = (name: string) => `\${vars.${name}}`;

interface VariableTableProps {
    variables: Variable[];
}

const VariableTable = ({variables}: VariableTableProps) => {
    const {setCurrentVariable, setShowDeleteDialog, setShowEditDialog} = useVariablesStore(
        useShallow((state) => ({
            setCurrentVariable: state.setCurrentVariable,
            setShowDeleteDialog: state.setShowDeleteDialog,
            setShowEditDialog: state.setShowEditDialog,
        }))
    );

    const {canManage} = useVariablesProvider();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const columns = useMemo(() => {
        const baseColumns = [
            columnHelper.accessor('name', {
                cell: (info) => <span className="font-mono">{info.getValue()}</span>,
                header: 'Name',
            }),
            columnHelper.accessor('value', {
                cell: (info) => {
                    const value = info.getValue();

                    if (value.length <= VALUE_TRUNCATE_LENGTH) {
                        return <span>{value}</span>;
                    }

                    return (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <span>{`${value.slice(0, VALUE_TRUNCATE_LENGTH)}…`}</span>
                            </TooltipTrigger>

                            <TooltipContent>{value}</TooltipContent>
                        </Tooltip>
                    );
                },
                header: 'Value',
            }),
            columnHelper.display({
                cell: (info) => {
                    const reference = toReference(info.row.original.name);

                    return (
                        <div className="flex items-center gap-1">
                            <code>{reference}</code>

                            <Button
                                aria-label="Copy reference"
                                icon={<CopyIcon className="size-4 text-content-neutral-tertiary" />}
                                onClick={() => copyToClipboard(reference)}
                                size="icon"
                                variant="ghost"
                            />
                        </div>
                    );
                },
                header: 'Reference',
                id: 'reference',
            }),
            columnHelper.accessor('lastModifiedDate', {
                cell: (info) => {
                    const value = info.getValue();

                    return value
                        ? `${new Date(value).toLocaleDateString()} ${new Date(value).toLocaleTimeString()}`
                        : '';
                },
                header: 'Last modified',
            }),
        ];

        if (!canManage) {
            return columnHelper.columns(baseColumns);
        }

        return columnHelper.columns([
            ...baseColumns,
            columnHelper.display({
                cell: (info) => (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                aria-label="Variable actions"
                                icon={<EllipsisVerticalIcon className="size-4" />}
                                size="icon"
                                variant="ghost"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem
                                onClick={() => {
                                    setCurrentVariable(info.row.original);
                                    setShowEditDialog(true);
                                }}
                            >
                                Edit
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                onClick={() => {
                                    setCurrentVariable(info.row.original);
                                    setShowDeleteDialog(true);
                                }}
                                variant="destructive"
                            >
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                ),
                header: '',
                id: 'actions',
            }),
        ]);
    }, [canManage, copyToClipboard, setCurrentVariable, setShowDeleteDialog, setShowEditDialog]);

    const reactTable = useTable({
        columns,
        data: variables,
        features: coreTableFeatures,
    });

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    return (
        <div className="w-full space-y-4 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
            <Table>
                <TableHeader>
                    {headerGroups.map((headerGroup) => (
                        <TableRow className="border-b-border/50" key={headerGroup.id}>
                            {headerGroup.headers.map((header) => (
                                <TableHead
                                    className="sticky top-0 z-10 bg-surface-neutral-primary p-3 text-left text-xs font-medium tracking-wide text-content-neutral-secondary uppercase"
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
                        <TableRow className="border-b-border/50" key={row.id}>
                            {row.getAllCells().map((cell) => (
                                <TableCell
                                    className={twMerge(
                                        'whitespace-nowrap',
                                        cell.id.endsWith('actions') && 'flex justify-end',
                                        cell.id.endsWith('name') && 'truncate xl:min-w-80'
                                    )}
                                    key={cell.id}
                                >
                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
};

export default VariableTable;
