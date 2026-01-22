import {Dispatch, SetStateAction} from 'react';

export type GridRowType = {id: string} & Record<string, unknown>;

export type SummaryRowType = object;

export interface BooleanCellRendererProps {
    columnName: string;
    onToggle: (rowId: string, columnName: string, value: boolean) => void;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
}
