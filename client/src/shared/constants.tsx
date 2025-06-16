import {
    CalculatorIcon,
    CalendarCheckIcon,
    CircleDollarSign,
    CodeIcon,
    DiamondPercentIcon,
    GaugeIcon,
    HandshakeIcon,
    HeadsetIcon,
    MessageCircleQuestionIcon,
    MessagesSquareIcon,
    Package2,
    SmileIcon,
    SparklesIcon,
    SquareKanbanIcon,
} from 'lucide-react';

import IconECommerce from '../assets/IconECommerce.svg';
import {getRandomId} from './util/random-utils';

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
    // NULL: 'NULL',
    NUMBER: 'NUMBER',
    OBJECT: 'OBJECT_BUILDER',
    STRING: 'TEXT',
    TIME: 'TIME',
};

export const CONDITION_CASE_TRUE = 'caseTrue';
export const CONDITION_CASE_FALSE = 'caseFalse';

export const FINAL_PLACEHOLDER_NODE_ID = getRandomId();

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
export const PATH_OPENING_PARENTHESIS_REPLACEMENT = '_OPENING_PARENTHESIS_';
export const PATH_CLOSING_PARENTHESIS_REPLACEMENT = '_CLOSING_PARENTHESIS_';
export const PATH_UNICODE_REPLACEMENT_PREFIX = '__UNICODE_';

export const NODE_WIDTH = 240;
export const NODE_HEIGHT = 100;
export const PLACEHOLDER_NODE_HEIGHT = 28;
export const ROOT_CLUSTER_WIDTH = 250;
export const DIRECTION = 'TB';
export const DEFAULT_NODE_POSITION = {x: 0, y: 0};

export const TASK_DISPATCHER_NAMES = [
    'branch',
    'condition',
    'each',
    'fork-join',
    'loop',
    'loopBreak',
    'map',
    'parallel',
    'subflow',
];

export const ROOT_CLUSTER_ELEMENT_NAMES = ['aiAgent'];

export const SORT_OPTIONS = [
    {
        label: 'Last edited',
        value: 'last-edited',
    },
    {
        label: 'Date created',
        value: 'date-created',
    },
    {
        label: 'A-Z',
        value: 'alphabetical',
    },
    {
        label: 'Z-A',
        value: 'reverse-alphabetical',
    },
];

export const MINIMAP_MASK_COLOR = '#f1f5f9';
export const MINIMAP_NODE_COLOR = '#e2e8f0';

export const TASK_DISPATCHER_SUBTASK_COLLECTIONS = {
    branch: ['default', 'cases'],
    condition: ['caseTrue', 'caseFalse'],
    each: ['iteratee'],
    loop: ['iteratee'],
    parallel: ['tasks'],
};

export const COMPONENT_CATEGORY_ICON: Record<string, JSX.Element> = {
    accounting: <CalculatorIcon />,
    'artificial-intelligence': <SparklesIcon />,
    'calendars-and-scheduling': <CalendarCheckIcon />,
    communication: <MessagesSquareIcon />,
    crm: <HandshakeIcon />,
    'customer-support': <HeadsetIcon />,
    'developer-tools': <CodeIcon />,
    'e-commerce': <img alt="E-Commerce" src={IconECommerce} />,
    'file-storage': <Package2 />,
    helpers: <MessageCircleQuestionIcon />,
    'marketing-automation': <DiamondPercentIcon />,
    'payment-processing': <CircleDollarSign />,
    'productivity-and-collaboration': <GaugeIcon />,
    'project-management': <SquareKanbanIcon />,
    'surveys-and-feedback': <SmileIcon />,
};

export const TASK_DISPATCHER_DATA_KEY_MAP = {
    branch: 'branchData',
    condition: 'conditionData',
    each: 'eachData',
    loop: 'loopData',
    parallel: 'parallelData',
};
