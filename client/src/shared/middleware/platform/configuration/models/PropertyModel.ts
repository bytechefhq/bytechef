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
import type { PropertyTypeModel } from './PropertyTypeModel';
import {
    PropertyTypeModelFromJSON,
    PropertyTypeModelFromJSONTyped,
    PropertyTypeModelToJSON,
} from './PropertyTypeModel';

import {
     ArrayPropertyModelFromJSONTyped,
     BooleanPropertyModelFromJSONTyped,
     DatePropertyModelFromJSONTyped,
     DateTimePropertyModelFromJSONTyped,
     DynamicPropertiesPropertyModelFromJSONTyped,
     FileEntryPropertyModelFromJSONTyped,
     IntegerPropertyModelFromJSONTyped,
     NullPropertyModelFromJSONTyped,
     NumberPropertyModelFromJSONTyped,
     ObjectPropertyModelFromJSONTyped,
     StringPropertyModelFromJSONTyped,
     TaskPropertyModelFromJSONTyped,
     TimePropertyModelFromJSONTyped
} from './index';

/**
 * A base property.
 * @export
 * @interface PropertyModel
 */
export interface PropertyModel {
    /**
     * If the property should be grouped under advanced options.
     * @type {boolean}
     * @memberof PropertyModel
     */
    advancedOption?: boolean;
    /**
     * The property description.
     * @type {string}
     * @memberof PropertyModel
     */
    description?: string;
    /**
     * Defines rules when the property should be shown or hidden.
     * @type {string}
     * @memberof PropertyModel
     */
    displayCondition?: string;
    /**
     * Defines if the property can contain expressions or only constant values. Defaults to true.
     * @type {boolean}
     * @memberof PropertyModel
     */
    expressionEnabled?: boolean;
    /**
     * If the property should be visible or not.
     * @type {boolean}
     * @memberof PropertyModel
     */
    hidden?: boolean;
    /**
     * The property name.
     * @type {string}
     * @memberof PropertyModel
     */
    name?: string;
    /**
     * If the property is required or not.
     * @type {boolean}
     * @memberof PropertyModel
     */
    required?: boolean;
    /**
     * 
     * @type {PropertyTypeModel}
     * @memberof PropertyModel
     */
    type: PropertyTypeModel;
}

/**
 * Check if a given object implements the PropertyModel interface.
 */
export function instanceOfPropertyModel(value: object): boolean {
    if (!('type' in value)) return false;
    return true;
}

export function PropertyModelFromJSON(json: any): PropertyModel {
    return PropertyModelFromJSONTyped(json, false);
}

export function PropertyModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): PropertyModel {
    if (json == null) {
        return json;
    }
    if (!ignoreDiscriminator) {
        if (json['type'] === 'ARRAY') {
            return ArrayPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'BOOLEAN') {
            return BooleanPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'DATE') {
            return DatePropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'DATE_TIME') {
            return DateTimePropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'DYNAMIC_PROPERTIES') {
            return DynamicPropertiesPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'FILE_ENTRY') {
            return FileEntryPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'INTEGER') {
            return IntegerPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'NULL') {
            return NullPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'NUMBER') {
            return NumberPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'OBJECT') {
            return ObjectPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'STRING') {
            return StringPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'TASK') {
            return TaskPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'TIME') {
            return TimePropertyModelFromJSONTyped(json, true);
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
        'type': PropertyTypeModelFromJSON(json['type']),
    };
}

export function PropertyModelToJSON(value?: PropertyModel | null): any {
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
        'type': PropertyTypeModelToJSON(value['type']),
    };
}

