import {PropertyType} from '@/shared/middleware/platform/configuration';
import isObject from 'isobject';

export default function getParameterItemType(parameterItemValue: unknown): PropertyType {
    if (isObject(parameterItemValue)) {
        return 'OBJECT';
    } else if (Array.isArray(parameterItemValue)) {
        return 'ARRAY';
    } else if (typeof parameterItemValue === 'string') {
        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z$/.test(parameterItemValue)) {
            return 'DATE_TIME';
        } else if (/^\d{4}-\d{2}-\d{2}$/.test(parameterItemValue)) {
            return 'DATE';
        } else {
            return 'STRING';
        }
    } else if (typeof parameterItemValue === 'number') {
        if (/^\d+$/.test(parameterItemValue.toString())) {
            return 'INTEGER';
        } else {
            return 'NUMBER';
        }
    } else if (typeof parameterItemValue === 'boolean') {
        return 'BOOLEAN';
    } else {
        return 'STRING';
    }
}
