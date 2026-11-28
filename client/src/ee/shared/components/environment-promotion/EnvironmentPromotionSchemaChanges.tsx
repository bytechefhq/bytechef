import {PromotionSchemaChangeKind} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

import {EnvironmentPromotionPreviewType} from './hooks/useEnvironmentPromotionDialog';

type PromotionSchemaChangeType = EnvironmentPromotionPreviewType['schemaChanges'][number];

export interface EnvironmentPromotionSchemaChangesProps {
    schemaChanges: PromotionSchemaChangeType[];
}

const describeChange = (schemaChange: PromotionSchemaChangeType): string => {
    switch (schemaChange.kind) {
        case PromotionSchemaChangeKind.CreateTable:
            return 'New table';
        case PromotionSchemaChangeKind.AddColumn:
            return 'New column';
        case PromotionSchemaChangeKind.DropColumn:
            return 'Deletes data in this column';
        case PromotionSchemaChangeKind.ChangeColumnType:
            return `Type changes from ${schemaChange.fromType} to ${schemaChange.toType}`;
        case PromotionSchemaChangeKind.ChangeLink:
            return 'Changes the link; existing links may be cleared';
        default:
            return '';
    }
};

const EnvironmentPromotionSchemaChanges = ({schemaChanges}: EnvironmentPromotionSchemaChangesProps) => {
    const changesByTable = useMemo(() => {
        const groups = new Map<string, PromotionSchemaChangeType[]>();

        schemaChanges.forEach((schemaChange) => {
            const tableChanges = groups.get(schemaChange.tableName) ?? [];

            tableChanges.push(schemaChange);

            groups.set(schemaChange.tableName, tableChanges);
        });

        return Array.from(groups.entries());
    }, [schemaChanges]);

    return (
        <fieldset className="space-y-2 border-0">
            <legend className="text-sm font-medium">Schema changes</legend>

            {changesByTable.map(([tableName, tableChanges]) => (
                <div className="space-y-1" key={tableName}>
                    <span className="text-sm font-medium">{tableName}</span>

                    <ul className="space-y-1 text-sm text-content-neutral-secondary">
                        {tableChanges.map((schemaChange) => (
                            <li
                                className="flex items-center gap-2"
                                key={`${schemaChange.kind}-${schemaChange.columnName ?? ''}`}
                            >
                                {schemaChange.columnName && (
                                    <span className="font-medium">{schemaChange.columnName}</span>
                                )}

                                <span
                                    className={
                                        schemaChange.kind === PromotionSchemaChangeKind.DropColumn ||
                                        schemaChange.kind === PromotionSchemaChangeKind.ChangeLink
                                            ? 'text-content-destructive-primary'
                                            : undefined
                                    }
                                >
                                    {describeChange(schemaChange)}
                                </span>
                            </li>
                        ))}
                    </ul>
                </div>
            ))}
        </fieldset>
    );
};

export default EnvironmentPromotionSchemaChanges;
