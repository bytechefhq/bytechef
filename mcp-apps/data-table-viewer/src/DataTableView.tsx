import {twMerge} from 'tailwind-merge';

export interface DataTableDataI {
    name?: string;
    rows: Record<string, unknown>[];
}

function toCellText(value: unknown): string {
    if (value == null) {
        return '';
    }

    if (typeof value === 'object') {
        return JSON.stringify(value);
    }

    return String(value);
}

export default function DataTableView({data}: {data: DataTableDataI}) {
    const columns = Array.from(new Set(data.rows.flatMap((row) => Object.keys(row))));

    return (
        <div className="flex h-full flex-col overflow-hidden bg-surface-neutral-primary">
            {data.name ? (
                <div className="border-b border-stroke-neutral-secondary px-4 py-3 text-sm font-medium text-content-neutral-secondary">
                    {data.name}
                </div>
            ) : null}

            <div className="flex-1 overflow-auto">
                {data.rows.length === 0 || columns.length === 0 ? (
                    <div className="flex h-full items-center justify-center text-sm text-content-neutral-secondary">
                        No rows to display.
                    </div>
                ) : (
                    <table className="w-full border-collapse text-left text-sm">
                        <thead>
                            <tr>
                                {columns.map((column) => (
                                    <th
                                        className="sticky top-0 border-b border-stroke-neutral-tertiary bg-surface-neutral-primary-hover px-3 py-2 font-medium text-content-neutral-secondary"
                                        key={column}
                                    >
                                        {column}
                                    </th>
                                ))}
                            </tr>
                        </thead>

                        <tbody>
                            {data.rows.map((row, rowIndex) => (
                                <tr className={twMerge(rowIndex % 2 === 1 && 'bg-surface-neutral-primary-hover')} key={rowIndex}>
                                    {columns.map((column) => (
                                        <td
                                            className="border-b border-stroke-neutral-secondary px-3 py-2 align-top text-content-neutral-secondary"
                                            key={column}
                                        >
                                            {toCellText(row[column])}
                                        </td>
                                    ))}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}
