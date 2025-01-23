export const AUTHORITIES = {
    ADMIN: 'ROLE_ADMIN',
    USER: 'ROLE_USER',
};

export const VALUE_PROPERTY_CONTROL_TYPES = {
    ARRAY: 'ARRAY_BUILDER',
    BOOLEAN: 'SELECT',
    DATE: 'DATE',
    DATE_TIME: 'DATE_TIME',
    FILE_ENTRY: 'FILE_ENTRY',
    INTEGER: 'INTEGER',
    NULL: 'NULL',
    NUMBER: 'NUMBER',
    OBJECT: 'OBJECT_BUILDER',
    STRING: 'TEXT',
    TIME: 'TIME',
};

export const CONDITION_CASE_TRUE = 'caseTrue';
export const CONDITION_CASE_FALSE = 'caseFalse';

export const EDITOR_PLACEHOLDER = (
    <>
        <pre>{'//'}Write sample output value, for example:</pre>
        <pre>{'{'}</pre>
        <pre className="pl-4">{'"country": "USA"'}</pre>
        <pre className="pl-4">{'"people": ['}</pre>
        <pre className="pl-8">{'{'}</pre>
        <pre className="pl-12">{'"firstName": Joe'}</pre>
        <pre className="pl-12">{'"lastName": Jackson'}</pre>
        <pre className="pl-12">{'"gender": Male'}</pre>
        <pre className="pl-12">{'"age": 28'}</pre>
        <pre className="pl-12">{'"number": 7349282382'}</pre>
        <pre className="pl-8">{'}'}</pre>
        <pre className="pl-4">{']'}</pre>
        <pre>{'}'}</pre>
    </>
);

export const SPACE = 4;

const STROKE_GRAY_300 = '#D1D5DB';

export const EDGE_STYLES = {
    fill: 'none',
    stroke: STROKE_GRAY_300,
    strokeWidth: 2,
};

export const PATH_SPACE_REPLACEMENT = '_SPACE_';
export const PATH_DIGIT_PREFIX = '_DIGIT_';
export const PATH_DASH_REPLACEMENT = '_DASH_';
export const PATH_HASH_REPLACEMENT = '_HASH_';
