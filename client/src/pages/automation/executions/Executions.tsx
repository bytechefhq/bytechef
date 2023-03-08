/* eslint-disable react/jsx-key */
import LayoutContainer from 'layouts/LayoutContainer/LayoutContainer';
import PageHeader from 'components/PageHeader/PageHeader';
import {useMemo} from 'react';
import {useTable} from 'react-table';
import Input from 'components/Input/Input';
import {DATA} from './ExecutionsData';

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

    const Status = (): JSX.Element => (
        <div className="p-4">
            <h2 className="text-sm font-medium text-gray-500">Status</h2>

            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="text"
                name="Status"
            />
        </div>
    );

    const StartTimeInput = (): JSX.Element => (
        <div className="p-4">
            <h2 className="text-sm font-medium text-gray-500">Start time</h2>

            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="date"
                name="Start time"
            />
        </div>
    );

    const EndTimeInput = (): JSX.Element => (
        <div className="p-4">
            <h2 className="text-sm font-medium text-gray-500">End time</h2>

            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="date"
                name="End time"
            />
        </div>
    );

    const Projects = (): JSX.Element => (
        <div className="p-4">
            <h2 className="text-sm font-medium text-gray-500">Projects</h2>

            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="text"
                name="Projects"
            />
        </div>
    );

    const Workflows = (): JSX.Element => (
        <div className="p-4">
            <h2 className="text-sm font-medium text-gray-500">Workflows</h2>

            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="text"
                name="Workflows"
            />
        </div>
    );

    const Instances = (): JSX.Element => (
        <div className="p-4">
            <h2 className="text-sm font-medium text-gray-500">Instances</h2>

            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="text"
                name="Instances"
            />
        </div>
    );

    return (
        <LayoutContainer
            header={<PageHeader title="Workflow History" />}
            leftSidebarHeader={
                <>
                    <PageHeader leftSidebar title="Executions" />
                    <Status />
                    <StartTimeInput />
                    <EndTimeInput />
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
