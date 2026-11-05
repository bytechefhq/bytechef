import {afterEach, describe, expect, it, vi} from 'vitest';

import {OBJECT_SCHEMA_OPTIONS, STRING_SCHEMA_OPTIONS} from '../constants';
import {
    addSchemaProperty,
    deleteSchemaField,
    deleteSchemaProperty,
    fieldsToOptions,
    findOption,
    getAllSchemaKeys,
    getSchemaField,
    getSchemaFields,
    getSchemaMenuOptions,
    getSchemaRequiredProperties,
    getValidFields,
    hasSchemaItems,
    hasSchemaProperties,
    isEmpty,
    isFieldRequired,
    isSchemaArray,
    isSchemaObject,
    optionsToStrings,
    removeWrongFields,
    renameSchemaField,
    renameSchemaProperty,
    schemaFieldAsOption,
    schemaPropertiesAsOptions,
    schemaRequiredPropertiesAsOptions,
    setSchemaField,
    setSchemaProperty,
    setSchemaTypeAndRemoveWrongFields,
    stringsToOptions,
    translateLabels,
} from '../helpers';

describe('getSchemaField', () => {
    it('reads a dotted path', () => {
        expect(getSchemaField({a: {b: {c: 1}}}, 'a.b.c')).toBe(1);
    });

    it('reads an array path', () => {
        expect(getSchemaField({properties: {'first.name': {type: 'string'}}}, ['properties', 'first.name'])).toEqual({
            type: 'string',
        });
    });

    it('returns undefined when an intermediate key is missing', () => {
        expect(getSchemaField({a: {}}, 'a.b.c')).toBeUndefined();
    });

    it('returns undefined when an intermediate value is null', () => {
        expect(getSchemaField({a: null}, 'a.b')).toBeUndefined();
    });

    it('returns undefined for a missing root object', () => {
        expect(getSchemaField(undefined, 'type')).toBeUndefined();
    });

    it('keeps falsy leaf values that are defined', () => {
        expect(getSchemaField({minimum: 0}, 'minimum')).toBe(0);
        expect(getSchemaField({uniqueItems: false}, 'uniqueItems')).toBe(false);
    });
});

describe('getSchemaFields', () => {
    it('picks only keys that are defined on the object', () => {
        expect(getSchemaFields({a: 1, b: undefined, c: 3}, ['a', 'b', 'd'])).toEqual({a: 1});
    });

    it('returns an empty object when keys are missing', () => {
        expect(getSchemaFields({a: 1}, undefined as unknown as string[])).toEqual({});
    });
});

describe('getAllSchemaKeys', () => {
    it('lists the keys of a schema', () => {
        expect(getAllSchemaKeys({title: 'x', type: 'string'})).toEqual(['title', 'type']);
    });
});

describe('getSchemaRequiredProperties', () => {
    it('returns the properties named in required', () => {
        const schema = {
            properties: {age: {type: 'integer'}, name: {type: 'string'}},
            required: ['name', 'missing'],
            type: 'object',
        };

        expect(getSchemaRequiredProperties(schema)).toEqual({name: {type: 'string'}});
    });

    it('returns an empty object when nothing is required', () => {
        expect(getSchemaRequiredProperties({properties: {name: {}}, type: 'object'})).toEqual({});
    });
});

describe('setSchemaField', () => {
    it('sets a value at a dotted path, creating intermediate objects', () => {
        const schema: Record<string, unknown> = {};

        const result = setSchemaField('a.b.c', 5, schema);

        expect(result).toBe(schema);
        expect(schema).toEqual({a: {b: {c: 5}}});
    });

    it('keeps existing siblings on the path', () => {
        const schema = {properties: {name: {type: 'string'}}};

        setSchemaProperty('age', {type: 'integer'}, schema);

        expect(schema).toEqual({properties: {age: {type: 'integer'}, name: {type: 'string'}}});
    });

    it('treats an array path as literal keys', () => {
        const schema = {};

        setSchemaField(['properties', 'first.name'], {type: 'string'}, schema);

        expect(schema).toEqual({properties: {'first.name': {type: 'string'}}});
    });
});

describe('deleteSchemaField', () => {
    it('removes a nested key', () => {
        const schema = {properties: {age: {}, name: {}}};

        deleteSchemaField(['properties', 'age'], schema);

        expect(schema).toEqual({properties: {name: {}}});
    });

    it('removes a top-level key given as a string', () => {
        const schema = {minLength: 1, type: 'string'};

        deleteSchemaField('minLength', schema);

        expect(schema).toEqual({type: 'string'});
    });
});

describe('deleteSchemaProperty', () => {
    it('removes the property and drops it from required', () => {
        const schema = {properties: {age: {}, name: {}}, required: ['name', 'age'], type: 'object'};

        deleteSchemaProperty('age', schema);

        expect(schema).toEqual({properties: {name: {}}, required: ['name'], type: 'object'});
    });

    it('keeps the same required array when the key was not required', () => {
        const required = ['name'];
        const schema = {properties: {age: {}, name: {}}, required, type: 'object'};

        deleteSchemaProperty('age', schema);

        expect(schema.required).toBe(required);
    });

    it('does not add required when the schema has none', () => {
        const schema = {properties: {age: {}}, type: 'object'};

        deleteSchemaProperty('age', schema);

        expect(schema).toEqual({properties: {}, type: 'object'});
    });
});

describe('addSchemaProperty', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('adds an empty property keyed by the current timestamp', () => {
        vi.spyOn(Date, 'now').mockReturnValue(1234);

        const schema = {properties: {name: {}}, type: 'object'};

        addSchemaProperty(schema);

        expect(schema.properties).toEqual({__1234__: {}, name: {}});
    });
});

describe('renameSchemaField', () => {
    it('renames a key while preserving key order', () => {
        const renamed = renameSchemaField('b', 'renamed')({a: 1, b: 2, c: 3});

        expect(Object.keys(renamed)).toEqual(['a', 'renamed', 'c']);
        expect(renamed).toEqual({a: 1, c: 3, renamed: 2});
    });

    it('returns a copy when the key is absent', () => {
        const source = {a: 1};

        const renamed = renameSchemaField('missing', 'other')(source);

        expect(renamed).toEqual({a: 1});
        expect(renamed).not.toBe(source);
    });
});

describe('renameSchemaProperty', () => {
    it('renames the property and its entry in required', () => {
        const schema = {properties: {age: {}, name: {}}, required: ['name', 'age'], type: 'object'};

        renameSchemaProperty('name', 'fullName', schema);

        expect(Object.keys(schema.properties)).toEqual(['age', 'fullName']);
        expect(schema.required).toEqual(['fullName', 'age']);
    });

    it('leaves required untouched when the renamed key is not required', () => {
        const required = ['age'];
        const schema = {properties: {age: {}, name: {}}, required, type: 'object'};

        renameSchemaProperty('name', 'fullName', schema);

        expect(schema.required).toBe(required);
    });

    it('works without a required list', () => {
        const schema = {properties: {name: {}}, type: 'object'};

        renameSchemaProperty('name', 'fullName', schema);

        expect(schema).toEqual({properties: {fullName: {}}, type: 'object'});
    });
});

describe('isEmpty', () => {
    it.each([
        [null, true],
        [undefined, true],
        [{}, true],
        [[], true],
        ['', true],
        ['   ', true],
        ['text', false],
        [{a: 1}, false],
        [[1], false],
        [0, false],
        [false, false],
    ])('isEmpty(%j) is %s', (value, expected) => {
        expect(isEmpty(value)).toBe(expected);
    });
});

describe('schema type checks', () => {
    it('detects object and array schemas', () => {
        expect(isSchemaObject({type: 'object'})).toBe(true);
        expect(isSchemaObject({type: 'array'})).toBe(false);
        expect(isSchemaArray({type: 'array'})).toBe(true);
        expect(isSchemaArray({})).toBe(false);
    });

    it('detects whether a field is required', () => {
        expect(isFieldRequired('name', {required: ['name']})).toBe(true);
        expect(isFieldRequired('age', {required: ['name']})).toBe(false);
        expect(isFieldRequired('name', {})).toBe(false);
    });

    it('detects properties and items', () => {
        expect(hasSchemaProperties({properties: {name: {}}})).toBe(true);
        expect(hasSchemaProperties({properties: {}})).toBe(false);
        expect(hasSchemaProperties({})).toBe(false);
        expect(hasSchemaItems({items: {type: 'string'}})).toBe(true);
        expect(hasSchemaItems({items: {}})).toBe(false);
    });
});

describe('options helpers', () => {
    it('returns the menu options for a type', () => {
        expect(getSchemaMenuOptions('string')).toBe(STRING_SCHEMA_OPTIONS);
        expect(getSchemaMenuOptions('object')).toBe(OBJECT_SCHEMA_OPTIONS);
    });

    it('finds an option by value', () => {
        const options = [{value: 'a'}, {value: 'b'}];

        expect(findOption('b')(options)).toBe(options[1]);
        expect(findOption('c')(options)).toBeUndefined();
    });

    it('converts between options and strings', () => {
        expect(optionsToStrings([{value: 'a'}, {value: 'b'}])).toEqual(['a', 'b']);
        expect(stringsToOptions(['a'])).toEqual([{label: 'a', value: 'a'}]);
        expect(stringsToOptions(undefined as unknown as string[])).toEqual([]);
    });

    it('labels a field with its title when present', () => {
        expect(schemaFieldAsOption('name', {title: 'Full name'})).toEqual({label: 'Full name', value: 'name'});
    });

    it('labels a field with its key when the title is blank or missing', () => {
        expect(schemaFieldAsOption('name', {title: '  '})).toEqual({label: 'name', value: 'name'});
        expect(schemaFieldAsOption('name', {})).toEqual({label: 'name', value: 'name'});
    });

    it('maps fields to options', () => {
        expect(fieldsToOptions({age: {}, name: {title: 'Name'}})).toEqual([
            {label: 'age', value: 'age'},
            {label: 'Name', value: 'name'},
        ]);
    });

    it('lists all and required properties as options', () => {
        const schema = {
            properties: {age: {title: 'Age'}, name: {}},
            required: ['name'],
            type: 'object',
        };

        expect(schemaPropertiesAsOptions(schema)).toEqual([
            {label: 'Age', value: 'age'},
            {label: 'name', value: 'name'},
        ]);
        expect(schemaRequiredPropertiesAsOptions(schema)).toEqual([{label: 'name', value: 'name'}]);
    });

    it('translates option labels and tolerates a missing list', () => {
        const translate = (text: string) => `t(${text})`;

        expect(translateLabels(translate, [{label: 'a', value: 1}])).toEqual([{label: 't(a)', value: 1}]);
        expect(translateLabels(translate, undefined as unknown as unknown[])).toEqual([]);
    });
});

describe('removeWrongFields', () => {
    it('returns the valid fields for a type', () => {
        expect(getValidFields('boolean')).toEqual(['description', 'type', 'title']);
    });

    it('keeps only the fields valid for the schema type', () => {
        expect(removeWrongFields({maxLength: 3, minimum: 1, title: 'x', type: 'string'})).toEqual({
            maxLength: 3,
            title: 'x',
            type: 'string',
        });
    });

    it('returns an empty object for an unknown type', () => {
        expect(removeWrongFields({title: 'x'})).toEqual({});
    });
});

describe('setSchemaTypeAndRemoveWrongFields', () => {
    it('changes the type and drops fields that no longer apply', () => {
        const schema = {maxLength: 3, properties: {a: {}}, title: 'x', type: 'string'};

        expect(setSchemaTypeAndRemoveWrongFields('object', schema)).toEqual({
            properties: {a: {}},
            title: 'x',
            type: 'object',
        });
    });

    it('starts from an empty schema when none is given', () => {
        expect(setSchemaTypeAndRemoveWrongFields('integer', undefined as unknown as Record<string, unknown>)).toEqual({
            type: 'integer',
        });
    });
});
