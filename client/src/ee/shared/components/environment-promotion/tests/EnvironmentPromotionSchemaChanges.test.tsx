import {PromotionSchemaChangeKind} from '@/shared/middleware/graphql';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import EnvironmentPromotionSchemaChanges from '../EnvironmentPromotionSchemaChanges';

describe('EnvironmentPromotionSchemaChanges', () => {
    it('groups the changes by table name', () => {
        render(
            <EnvironmentPromotionSchemaChanges
                schemaChanges={[
                    {
                        columnName: null,
                        destructive: false,
                        fromType: null,
                        kind: PromotionSchemaChangeKind.CreateTable,
                        tableName: 'orders',
                        toType: null,
                    },
                    {
                        columnName: 'total',
                        destructive: false,
                        fromType: null,
                        kind: PromotionSchemaChangeKind.AddColumn,
                        tableName: 'orders',
                        toType: 'NUMBER',
                    },
                    {
                        columnName: 'email',
                        destructive: false,
                        fromType: null,
                        kind: PromotionSchemaChangeKind.AddColumn,
                        tableName: 'customers',
                        toType: 'STRING',
                    },
                ]}
            />
        );

        expect(screen.getAllByText('orders')).toHaveLength(1);
        expect(screen.getAllByText('customers')).toHaveLength(1);
        expect(screen.getByText('New table')).toBeInTheDocument();
        expect(screen.getAllByText('New column')).toHaveLength(2);
    });

    it('shows the destructive label for a dropped column', () => {
        render(
            <EnvironmentPromotionSchemaChanges
                schemaChanges={[
                    {
                        columnName: 'legacy',
                        destructive: true,
                        fromType: 'STRING',
                        kind: PromotionSchemaChangeKind.DropColumn,
                        tableName: 'orders',
                        toType: null,
                    },
                ]}
            />
        );

        expect(screen.getByText('legacy')).toBeInTheDocument();
        expect(screen.getByText('Deletes data in this column')).toBeInTheDocument();
    });

    it('shows the link label for a changed link', () => {
        render(
            <EnvironmentPromotionSchemaChanges
                schemaChanges={[
                    {
                        columnName: 'customer',
                        destructive: false,
                        fromType: null,
                        kind: PromotionSchemaChangeKind.ChangeLink,
                        tableName: 'orders',
                        toType: null,
                    },
                ]}
            />
        );

        expect(screen.getByText('Changes the link; existing links may be cleared')).toBeInTheDocument();
    });

    it('shows the old and new type for a changed column type', () => {
        render(
            <EnvironmentPromotionSchemaChanges
                schemaChanges={[
                    {
                        columnName: 'total',
                        destructive: false,
                        fromType: 'STRING',
                        kind: PromotionSchemaChangeKind.ChangeColumnType,
                        tableName: 'orders',
                        toType: 'NUMBER',
                    },
                ]}
            />
        );

        expect(screen.getByText('Type changes from STRING to NUMBER')).toBeInTheDocument();
    });
});
