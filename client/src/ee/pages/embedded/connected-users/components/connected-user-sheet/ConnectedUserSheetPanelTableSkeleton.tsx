import {Skeleton} from '@/components/ui/skeleton';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {
    EXECUTIONS_TABLE_CELL_CLASS_NAME,
    EXECUTIONS_TABLE_HEADER_CLASS_NAME,
    EXECUTIONS_TABLE_HEAD_CLASS_NAME,
    EXECUTIONS_TABLE_STRIPED_ROW_CLASS_NAME,
    PINNED_CELL_CLASS_NAME,
    PINNED_HEAD_CLASS_NAME,
    PINNED_STRIPED_CELL_CLASS_NAME,
} from '@/shared/components/workflow-executions/workflowExecutionsTableClassNames';
import {twMerge} from 'tailwind-merge';

export interface ConnectedUserSheetPanelTableSkeletonColumnI {
    centered?: boolean;
    header: string;
    pinned?: boolean;
    skeletonClassName: string;
}

interface ConnectedUserSheetPanelTableSkeletonProps {
    columns: ConnectedUserSheetPanelTableSkeletonColumnI[];
    label: string;
    rowCount?: number;
}

const ConnectedUserSheetPanelTableSkeleton = ({
    columns,
    label,
    rowCount = 5,
}: ConnectedUserSheetPanelTableSkeletonProps) => (
    <div aria-busy="true" aria-label={label} role="status">
        <Table>
            <TableHeader className={EXECUTIONS_TABLE_HEADER_CLASS_NAME}>
                <TableRow>
                    {columns.map((column) => (
                        <TableHead
                            className={twMerge(
                                EXECUTIONS_TABLE_HEAD_CLASS_NAME,
                                column.centered && 'text-center',
                                column.pinned && PINNED_HEAD_CLASS_NAME
                            )}
                            key={column.header}
                        >
                            {column.header}
                        </TableHead>
                    ))}
                </TableRow>
            </TableHeader>

            <TableBody>
                {Array.from({length: rowCount}, (_, rowIndex) => (
                    <TableRow
                        className={twMerge('group border-0', EXECUTIONS_TABLE_STRIPED_ROW_CLASS_NAME)}
                        key={rowIndex}
                    >
                        {columns.map((column) => (
                            <TableCell
                                className={twMerge(
                                    EXECUTIONS_TABLE_CELL_CLASS_NAME,
                                    column.pinned && PINNED_CELL_CLASS_NAME,
                                    column.pinned && PINNED_STRIPED_CELL_CLASS_NAME
                                )}
                                key={column.header}
                            >
                                <Skeleton
                                    className={twMerge(
                                        'h-4',
                                        (column.centered || column.pinned) && 'mx-auto',
                                        column.skeletonClassName
                                    )}
                                />
                            </TableCell>
                        ))}
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    </div>
);

export default ConnectedUserSheetPanelTableSkeleton;
