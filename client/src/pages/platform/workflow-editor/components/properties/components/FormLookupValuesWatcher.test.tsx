import {act, render} from '@/shared/util/test-utils';
import {FieldValues, useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import FormLookupValuesWatcher from './FormLookupValuesWatcher';

/**
 * Host harness. Renders a real react-hook-form instance so FormLookupValuesWatcher's
 * useWatch subscribes to genuine form updates. Exposes the form's setValue via ref so tests
 * can drive input changes.
 */
const Harness = ({
    arrayIndex,
    defaultValues,
    optionsLookupDependsOn,
    propertiesLookupDependsOn,
    setLookupDependsOnValues,
    setValueRef,
}: {
    arrayIndex?: number;
    defaultValues: FieldValues;
    optionsLookupDependsOn?: Array<string>;
    propertiesLookupDependsOn?: Array<string>;
    setLookupDependsOnValues: (values: Array<unknown> | undefined) => void;
    setValueRef: {current: ((path: string, value: unknown) => void) | null};
}) => {
    const form = useForm({defaultValues});

    setValueRef.current = (path, value) => form.setValue(path, value, {shouldDirty: true});

    return (
        <FormLookupValuesWatcher
            arrayIndex={arrayIndex}
            control={form.control}
            optionsLookupDependsOn={optionsLookupDependsOn}
            propertiesLookupDependsOn={propertiesLookupDependsOn}
            setLookupDependsOnValues={setLookupDependsOnValues}
        />
    );
};

function createSetValueRef() {
    return {current: null as ((path: string, value: unknown) => void) | null};
}

describe('FormLookupValuesWatcher', () => {
    it('reports initial form values so dynamic-property queries can run on first render', () => {
        const setLookupDependsOnValues = vi.fn();
        const setValueRef = createSetValueRef();

        render(
            <Harness
                defaultValues={{connectionId: 42, spreadsheetId: 'sheet-1'}}
                propertiesLookupDependsOn={['connectionId', 'spreadsheetId']}
                setLookupDependsOnValues={setLookupDependsOnValues}
                setValueRef={setValueRef}
            />
        );

        expect(setLookupDependsOnValues).toHaveBeenLastCalledWith([42, 'sheet-1']);
    });

    it('pushes new lookup values as the user edits dependency fields — the core #4 fix', () => {
        const setLookupDependsOnValues = vi.fn();
        const setValueRef = createSetValueRef();

        render(
            <Harness
                defaultValues={{dependencyA: 'initial'}}
                propertiesLookupDependsOn={['dependencyA']}
                setLookupDependsOnValues={setLookupDependsOnValues}
                setValueRef={setValueRef}
            />
        );

        expect(setLookupDependsOnValues).toHaveBeenLastCalledWith(['initial']);

        act(() => {
            setValueRef.current!('dependencyA', 'changed');
        });

        expect(setLookupDependsOnValues).toHaveBeenLastCalledWith(['changed']);
    });

    it('expands [index] in paths using the provided arrayIndex so array-item forms resolve the right field', () => {
        const setLookupDependsOnValues = vi.fn();
        const setValueRef = createSetValueRef();

        render(
            <Harness
                arrayIndex={2}
                defaultValues={{items: [{name: 'a'}, {name: 'b'}, {name: 'c'}]}}
                propertiesLookupDependsOn={['items[index].name']}
                setLookupDependsOnValues={setLookupDependsOnValues}
                setValueRef={setValueRef}
            />
        );

        expect(setLookupDependsOnValues).toHaveBeenLastCalledWith(['c']);
    });

    it('masks fromAi expressions as undefined to match non-controlled useProperty behavior', () => {
        const setLookupDependsOnValues = vi.fn();
        const setValueRef = createSetValueRef();

        render(
            <Harness
                defaultValues={{dependencyA: "=fromAi('x', 'STRING', {})"}}
                propertiesLookupDependsOn={['dependencyA']}
                setLookupDependsOnValues={setLookupDependsOnValues}
                setValueRef={setValueRef}
            />
        );

        expect(setLookupDependsOnValues).toHaveBeenLastCalledWith([undefined]);
    });

    it('falls back to optionsLookupDependsOn when no propertiesLookupDependsOn is provided', () => {
        const setLookupDependsOnValues = vi.fn();
        const setValueRef = createSetValueRef();

        render(
            <Harness
                defaultValues={{sourceOption: 'picked'}}
                optionsLookupDependsOn={['sourceOption']}
                setLookupDependsOnValues={setLookupDependsOnValues}
                setValueRef={setValueRef}
            />
        );

        expect(setLookupDependsOnValues).toHaveBeenLastCalledWith(['picked']);
    });

    it('does not invoke setLookupDependsOnValues when neither lookup-dependency list is provided', () => {
        const setLookupDependsOnValues = vi.fn();
        const setValueRef = createSetValueRef();

        render(
            <Harness
                defaultValues={{ignored: 'value'}}
                setLookupDependsOnValues={setLookupDependsOnValues}
                setValueRef={setValueRef}
            />
        );

        expect(setLookupDependsOnValues).not.toHaveBeenCalled();
    });
});
