/// <reference types="vite-plugin-svgr/client" />

import AnyIcon from 'assets/any.svg';
import ArrayIcon from 'assets/array.svg';
import BooleanIcon from 'assets/boolean.svg';
import DateIcon from 'assets/date.svg';
import DateTimeIcon from 'assets/datetime.svg';
import DynamicIcon from 'assets/dynamic.svg';
import IntegerIcon from 'assets/integer.svg';
import NullIcon from 'assets/null.svg';
import NumberIcon from 'assets/number.svg';
import ObjectIcon from 'assets/object.svg';
import StringIcon from 'assets/string.svg';
import TimeIcon from 'assets/time.svg';

export const TYPE_ICONS = {
    ANY: (
        <span className="h-4 w-4 text-gray-600">
            <AnyIcon />
        </span>
    ),
    ARRAY: (
        <span className="h-4 w-4 text-gray-600">
            <ArrayIcon />
        </span>
    ),
    BOOLEAN: (
        <span className="h-4 w-4 text-gray-600">
            <BooleanIcon />
        </span>
    ),
    DATE: (
        <span className="h-4 w-4 text-gray-600">
            <DateIcon />
        </span>
    ),
    DATE_TIME: (
        <span className="h-4 w-4 text-gray-600">
            <DateTimeIcon />
        </span>
    ),
    DYNAMIC_PROPERTIES: (
        <span className="h-4 w-4 text-gray-600">
            <DynamicIcon />
        </span>
    ),
    INTEGER: (
        <span className="h-4 w-4 text-gray-600">
            <IntegerIcon />
        </span>
    ),
    NULL: (
        <span className="h-4 w-4 text-gray-600">
            <NullIcon />
        </span>
    ),
    NUMBER: (
        <span className="h-4 w-4 text-gray-600">
            <NumberIcon />
        </span>
    ),
    OBJECT: (
        <span className="h-4 w-4 text-gray-600">
            <ObjectIcon />
        </span>
    ),
    STRING: (
        <span className="h-4 w-4 text-gray-600">
            <StringIcon />
        </span>
    ),
    TIME: (
        <span className="h-4 w-4 text-gray-600">
            <TimeIcon />
        </span>
    ),
};
