import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import JsonSchemaBuilder from '../JsonSchemaBuilder';
import {SchemaRecordType} from '../utils/types';

const hoisted = vi.hoisted(() => ({
    receivedSchemas: [] as SchemaRecordType[],
}));

vi.mock('@/components/JsonSchemaBuilder/components/SchemaCreator', () => ({
    default: ({schema}: {schema: SchemaRecordType}) => {
        hoisted.receivedSchemas.push(schema);

        return <div data-testid="schema-creator">{JSON.stringify(schema)}</div>;
    },
}));

const nameSchema: SchemaRecordType = {properties: {name: {type: 'string'}}, type: 'object'};

const emailSchema: SchemaRecordType = {properties: {email: {type: 'string'}}, type: 'object'};

describe('JsonSchemaBuilder', () => {
    it('renders the initial schema without waiting for an effect', () => {
        hoisted.receivedSchemas.length = 0;

        render(<JsonSchemaBuilder schema={nameSchema} />);

        expect(hoisted.receivedSchemas[0]).toEqual(nameSchema);
    });

    it('shows a schema applied from outside, such as by the Copilot', () => {
        const {rerender} = render(<JsonSchemaBuilder schema={nameSchema} />);

        rerender(<JsonSchemaBuilder schema={emailSchema} />);

        expect(screen.getByTestId('schema-creator').textContent).toContain('email');
    });

    it('keeps the current schema object when the incoming schema is structurally the same', () => {
        const {rerender} = render(<JsonSchemaBuilder schema={nameSchema} />);

        hoisted.receivedSchemas.length = 0;

        rerender(<JsonSchemaBuilder schema={{...nameSchema}} />);

        expect(new Set(hoisted.receivedSchemas).size).toBe(1);
    });
});
