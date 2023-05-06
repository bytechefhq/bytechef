import {
    CellContext,
    createColumnHelper,
    flexRender,
    getCoreRowModel,
    useReactTable,
} from '@tanstack/react-table';
import Badge from 'components/Badge/Badge';
import {
    JobBasicModel,
    ProjectExecutionModel,
} from 'middleware/automation/project';
import useExecutionDetailsDialogStore from 'pages/automation/project/stores/useExecutionDetailsDialogStore';

const getDuration = (
    info: CellContext<ProjectExecutionModel, JobBasicModel | undefined>
) => {
    const infoValue = info.getValue();

    const startDate = infoValue?.startDate?.getTime();
    const endDate = infoValue?.endDate?.getTime();

    if (startDate && endDate) {
        return `${Math.round((endDate - startDate) / 1000)}s`;
    }
};

const columnHelper = createColumnHelper<ProjectExecutionModel>();

const columns = [
    columnHelper.accessor((row) => row.job, {
        header: 'Status',
        cell: (info) => (
            <Badge
                color={
                    info.getValue()?.status === 'COMPLETED'
                        ? 'green'
                        : info.getValue()?.status === 'FAILED'
                        ? 'red'
                        : 'default'
                }
                text={info.getValue()?.status ?? ''}
            />
        ),
    }),
    columnHelper.accessor('project', {
        header: 'Project',
        cell: (info) => info.getValue()?.name,
    }),
    columnHelper.accessor('workflow', {
        header: 'Workflow',
        cell: (info) => info.getValue()?.label,
    }),
    columnHelper.accessor('instance', {
        header: 'Instance',
        cell: (info) => info.getValue()?.name,
    }),
    columnHelper.accessor((row) => row.job, {
        header: 'Duration',
        cell: (info) => getDuration(info),
    }),
    columnHelper.accessor((row) => row.job, {
        header: 'Completed date',
        cell: (info) => (
            <>
                {info.getValue()?.endDate &&
                    `${info.getValue()?.endDate?.toLocaleDateString()} ${info
                        .getValue()
                        ?.endDate?.toLocaleTimeString()}`}
            </>
        ),
    }),
];

const ExecutionsTable = ({data}: {data: ProjectExecutionModel[]}) => {
    const reactTable = useReactTable<ProjectExecutionModel>({
        data,
        columns,
        getCoreRowModel: getCoreRowModel(),
    });

    const {setCurrentExecutionId, setExecutionDetailsDialogOpen} =
        useExecutionDetailsDialogStore();

    const headerGroups = reactTable.getHeaderGroups();
    const rows = reactTable.getRowModel().rows;

    const handleRowClick = (index: number) => {
        if (data[index].id) {
            setCurrentExecutionId(data[index].id!);

            setExecutionDetailsDialogOpen(true);
        }
    };

    return (
        <table className="w-full divide-y divide-gray-300 bg-white text-sm">
            <thead>
                {headerGroups.map((headerGroup) => (
                    <tr key={headerGroup.id}>
                        {headerGroup.headers.map((header) => (
                            <th
                                key={header.id}
                                className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"
                            >
                                {!header.isPlaceholder &&
                                    flexRender(
                                        header.column.columnDef.header,
                                        header.getContext()
                                    )}
                            </th>
                        ))}
                    </tr>
                ))}
            </thead>

            <tbody className="divide-y divide-gray-200 bg-white">
                {rows.map((row) => (
                    <tr
                        className="cursor-pointer"
                        key={row.id}
                        onClick={() => handleRowClick(row.index)}
                    >
                        {row.getVisibleCells().map((cell) => (
                            <td
                                key={cell.id}
                                className="whitespace-nowrap px-3 py-4 text-sm text-gray-900"
                            >
                                {flexRender(
                                    cell.column.columnDef.cell,
                                    cell.getContext()
                                )}
                            </td>
                        ))}
                    </tr>
                ))}
            </tbody>
        </table>
    );
};

export default ExecutionsTable;
