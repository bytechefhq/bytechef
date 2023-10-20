/* eslint-disable react/jsx-key */
import LayoutContainer from 'layouts/LayoutContainer/LayoutContainer';
import PageHeader from 'components/PageHeader/PageHeader';
import {useMemo} from 'react';
import {useTable} from 'react-table';

type Data = {
    id: number;
    date: string;
    status: string;
    project: string;
    workflow: string;
    instance: string;
};

const DATA: Data[] = [
    {
        id: 1,
        project: 'Project one',
        workflow: 'Workflow 1',
        instance: 'Instance 1',
        date: '01/01/2023',
        status: 'Completed',
    },
    {
        id: 2,
        project: 'Project two',
        workflow: 'Workflow 2',
        instance: 'Instance 2',
        date: '02/01/2023',
        status: 'Failed',
    },
    {
        id: 3,
        project: 'Project three',
        workflow: 'Workflow 3',
        instance: 'Instance 3',
        date: '03/01/2023',
        status: 'Running',
    },
];

export const Executions = () => {
    const columns = useMemo(
        () => [
            {
                Header: 'Status',
                accessor: 'status',
            },
            {
                Header: 'Project',
                accessor: 'project',
            },
            {
                Header: 'Workflow',
                accessor: 'workflow',
            },
            {
                Header: 'Instance',
                accessor: 'instance',
            },
            {
                Header: 'Completed time',
                accessor: 'date',
            },
        ],
        []
    );

    const tableInstance = useTable({
        columns,
        data: DATA,
    });

    const {getTableProps, getTableBodyProps, headerGroups, rows, prepareRow} =
        tableInstance;

    const Status = () => {
        return (
            <div className="p-4">
                <h2 className="text-sm font-medium text-gray-500">Status</h2>
                <input
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                    type="text"
                />
            </div>
        );
    };

    const StartTime = () => {
        return (
            <div className="p-4">
                <h2 className="text-sm font-medium text-gray-500">
                    Start time
                </h2>
                <input
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                    type="date"
                />
            </div>
        );
    };

    const EndTime = () => {
        return (
            <div className="p-4">
                <h2 className="text-sm font-medium text-gray-500">End time</h2>
                <input
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                    type="date"
                />
            </div>
        );
    };

    const Projects = () => {
        return (
            <div className="p-4">
                <h2 className="text-sm font-medium text-gray-500">Projects</h2>
                <input
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                    type="text"
                />
            </div>
        );
    };

    const Workflows = () => {
        return (
            <div className="p-4">
                <h2 className="text-sm font-medium text-gray-500">Workflows</h2>
                <input
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                    type="text"
                />
            </div>
        );
    };

    const Instances = () => {
        return (
            <div className="p-4">
                <h2 className="text-sm font-medium text-gray-500">Instances</h2>
                <input
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                    type="text"
                />
            </div>
        );
    };

    return (
        <LayoutContainer
            header={<PageHeader title="Workflow History" />}
            leftSidebarHeader={
                <>
                    <PageHeader leftSidebar title="Executions" />
                    <Status />
                    <StartTime />
                    <EndTime />
                    <Projects />
                    <Workflows />
                    <Instances />
                </>
            }
        >
            <table {...getTableProps()} className="mx-auto w-3/4 border-4">
                <thead>
                    {headerGroups.map((headerGroup) => (
                        <tr
                            {...headerGroup.getHeaderGroupProps()}
                            className="bg-gray-200"
                        >
                            {headerGroup.headers.map((column) => (
                                <th
                                    {...column.getHeaderProps()}
                                    className="border px-4 py-2 font-medium uppercase text-gray-600"
                                >
                                    {column.render('Header')}
                                </th>
                            ))}
                        </tr>
                    ))}
                </thead>

                <tbody {...getTableBodyProps()}>
                    {rows.map((row) => {
                        prepareRow(row);
                        return (
                            <tr
                                {...row.getRowProps()}
                                className="hover:bg-gray-100"
                            >
                                {row.cells.map((cell) => (
                                    <td
                                        {...cell.getCellProps()}
                                        className="border border-gray-300 px-4 py-2"
                                    >
                                        {cell.render('Cell')}
                                    </td>
                                ))}
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </LayoutContainer>
    );
};

export default Executions;
