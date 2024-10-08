/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration Internal API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
import type { PropertyType } from './PropertyType';
import {
    PropertyTypeFromJSON,
    PropertyTypeFromJSONTyped,
    PropertyTypeToJSON,
} from './PropertyType';

import { ArrayPropertyFromJSONTyped } from './ArrayProperty';
import { BooleanPropertyFromJSONTyped } from './BooleanProperty';
import { DatePropertyFromJSONTyped } from './DateProperty';
import { DateTimePropertyFromJSONTyped } from './DateTimeProperty';
import { DynamicPropertiesPropertyFromJSONTyped } from './DynamicPropertiesProperty';
import { FileEntryPropertyFromJSONTyped } from './FileEntryProperty';
import { IntegerPropertyFromJSONTyped } from './IntegerProperty';
import { NullPropertyFromJSONTyped } from './NullProperty';
import { NumberPropertyFromJSONTyped } from './NumberProperty';
import { ObjectPropertyFromJSONTyped } from './ObjectProperty';
import { StringPropertyFromJSONTyped } from './StringProperty';
import { TaskPropertyFromJSONTyped } from './TaskProperty';
import { TimePropertyFromJSONTyped } from './TimeProperty';
/**
 * A base property.
 * @export
 * @interface Property
 */
export interface Property {
    /**
     * If the property should be grouped under advanced options.
     * @type {boolean}
     * @memberof Property
     */
    advancedOption?: boolean;
    /**
     * The property description.
     * @type {string}
     * @memberof Property
     */
    description?: string;
    /**
     * Defines rules when the property should be shown or hidden.
     * @type {string}
     * @memberof Property
     */
    displayCondition?: string;
    /**
     * Defines if the property can contain expressions or only constant values. Defaults to true.
     * @type {boolean}
     * @memberof Property
     */
    expressionEnabled?: boolean;
    /**
     * If the property should be visible or not.
     * @type {boolean}
     * @memberof Property
     */
    hidden?: boolean;
    /**
     * The property name.
     * @type {string}
     * @memberof Property
     */
    name?: string;
    /**
     * If the property is required or not.
     * @type {boolean}
     * @memberof Property
     */
    required?: boolean;
    /**
     * 
     * @type {PropertyType}
     * @memberof Property
     */
    type: PropertyType;
}



/**
 * Check if a given object implements the Property interface.
 */
export function instanceOfProperty(value: object): value is Property {
    if (!('type' in value) || value['type'] === undefined) return false;
    return true;
}

export function PropertyFromJSON(json: any): Property {
    return PropertyFromJSONTyped(json, false);
}

export function PropertyFromJSONTyped(json: any, ignoreDiscriminator: boolean): Property {
    if (json == null) {
        return json;
    }
    if (!ignoreDiscriminator) {
        if (json['type'] === 'ARRAY') {
            return ArrayPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'BOOLEAN') {
            return BooleanPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'DATE') {
            return DatePropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'DATE_TIME') {
            return DateTimePropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'DYNAMIC_PROPERTIES') {
            return DynamicPropertiesPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'FILE_ENTRY') {
            return FileEntryPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'INTEGER') {
            return IntegerPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'NULL') {
            return NullPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'NUMBER') {
            return NumberPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'OBJECT') {
            return ObjectPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'STRING') {
            return StringPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'TASK') {
            return TaskPropertyFromJSONTyped(json, true);
        }
        if (json['type'] === 'TIME') {
            return TimePropertyFromJSONTyped(json, true);
        }
    }
    return {
        
        'advancedOption': json['advancedOption'] == null ? undefined : json['advancedOption'],
        'description': json['description'] == null ? undefined : json['description'],
        'displayCondition': json['displayCondition'] == null ? undefined : json['displayCondition'],
        'expressionEnabled': json['expressionEnabled'] == null ? undefined : json['expressionEnabled'],
        'hidden': json['hidden'] == null ? undefined : json['hidden'],
        'name': json['name'] == null ? undefined : json['name'],
        'required': json['required'] == null ? undefined : json['required'],
        'type': PropertyTypeFromJSON(json['type']),
    };
}

export function PropertyToJSON(value?: Property | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'advancedOption': value['advancedOption'],
        'description': value['description'],
        'displayCondition': value['displayCondition'],
        'expressionEnabled': value['expressionEnabled'],
        'hidden': value['hidden'],
        'name': value['name'],
        'required': value['required'],
        'type': PropertyTypeToJSON(value['type']),
    };
}

