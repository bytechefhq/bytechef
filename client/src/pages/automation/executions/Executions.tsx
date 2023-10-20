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

    const StatusInput = (): JSX.Element => (
        <div className="p-4">
            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                name="Status"
                label="Status"
            />
        </div>
    );

    const StartTimeInput = (): JSX.Element => (
        <div className="p-4">
            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="date"
                name="Start time"
                label="Start time"
            />
        </div>
    );

    const EndTimeInput = (): JSX.Element => (
        <div className="p-4">
            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                type="date"
                name="End time"
                label="End time"
            />
        </div>
    );

    const ProjectsInput = (): JSX.Element => (
        <div className="p-4">
            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                name="Projects"
                label="Projects"
            />
        </div>
    );

    const WorkflowsInput = (): JSX.Element => (
        <div className="p-4">
            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                name="Workflows"
                label="Workflows"
            />
        </div>
    );

    const InstancesInput = (): JSX.Element => (
        <div className="p-4">
            <Input
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                name="Instances"
                label="Instances"
            />
        </div>
    );

    return (
        <LayoutContainer
            header={<PageHeader title="Workflow History" />}
            leftSidebarHeader={
                <>
                    <PageHeader leftSidebar title="Executions" />

                    <StatusInput />

                    <StartTimeInput />

                    <EndTimeInput />

                    <ProjectsInput />

                    <WorkflowsInput />

                    <InstancesInput />
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
