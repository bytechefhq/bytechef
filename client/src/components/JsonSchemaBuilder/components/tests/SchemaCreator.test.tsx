import {i18n} from '@lingui/core';
import {I18nProvider} from '@lingui/react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import JsonSchemaBuilder from '../../JsonSchemaBuilder';
import {SchemaRecordType} from '../../utils/types';

vi.mock('../SchemaMenuPopover', () => ({
    default: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('../SchemaTypesSelect', () => ({
    default: ({type}: {type: string}) => <div data-testid="type-select">{type}</div>,
}));

i18n.load('en', {});
i18n.activate('en');

const wrapper = ({children}: {children: ReactNode}) => <I18nProvider i18n={i18n}>{children}</I18nProvider>;

describe('SchemaCreator', () => {
    it('object root surfaces an add button so children can be added', () => {
        const schema: SchemaRecordType = {properties: {}, required: [], type: 'object'};

        render(<JsonSchemaBuilder onChange={vi.fn()} schema={schema} />, {wrapper});

        expect(screen.queryByTitle('Add Property')).toBeInTheDocument();
    });

    it('array root with untyped items surfaces an add button so children can be added', () => {
        const schema: SchemaRecordType = {type: 'array'};

        render(<JsonSchemaBuilder onChange={vi.fn()} schema={schema} />, {wrapper});

        const hasAddButton = screen.queryByTitle('Add') !== null || screen.queryByTitle('Add Property') !== null;

        expect(hasAddButton).toBe(true);
    });

    it('array root adds a child property to the items object when add is clicked', () => {
        const onChange = vi.fn();

        const schema: SchemaRecordType = {items: {properties: {}, type: 'object'}, type: 'array'};

        render(<JsonSchemaBuilder onChange={onChange} schema={schema} />, {wrapper});

        fireEvent.click(screen.getByTitle('Add'));

        const last = onChange.mock.calls.at(-1)?.[0] as SchemaRecordType;
        const items = last.items as SchemaRecordType;

        expect(Object.keys(items?.properties ?? {}).length).toBe(1);
    });
});
