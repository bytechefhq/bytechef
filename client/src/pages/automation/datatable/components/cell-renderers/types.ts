import {Dispatch, SetStateAction} from 'react';

export type GridRowType = {id: string} & Record<string, unknown>;

export type SummaryRowType = object;

export interface CellRendererProps {
    columnName: string;
    tableId: string;
    environmentId: string;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
    updateRowMutation: {
        mutate: (params: {
            input: {
                environmentId: string;
                id: string;
                tableId: string;
                values: Record<string, unknown>;
            };
        }) => void;
    };
}
