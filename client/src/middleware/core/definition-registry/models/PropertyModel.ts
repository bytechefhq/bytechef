/* tslint:disable */
/* eslint-disable */
/**
 * Core Definition API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
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
     IntegerPropertyModelFromJSONTyped,
     NullPropertyModelFromJSONTyped,
     NumberPropertyModelFromJSONTyped,
     ObjectPropertyModelFromJSONTyped,
     OneOfPropertyModelFromJSONTyped,
     StringPropertyModelFromJSONTyped
} from './';

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
     * Defines rules when a property should be shown or hidden.
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
     * The property label.
     * @type {string}
     * @memberof PropertyModel
     */
    label?: string;
    /**
     * The property name.
     * @type {string}
     * @memberof PropertyModel
     */
    name?: string;
    /**
     * The property placeholder.
     * @type {string}
     * @memberof PropertyModel
     */
    placeholder?: string;
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
    type?: PropertyTypeModel;
}

/**
 * Check if a given object implements the PropertyModel interface.
 */
export function instanceOfPropertyModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PropertyModelFromJSON(json: any): PropertyModel {
    return PropertyModelFromJSONTyped(json, false);
}

export function PropertyModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): PropertyModel {
    if ((json === undefined) || (json === null)) {
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
        if (json['type'] === 'ONE_OF') {
            return OneOfPropertyModelFromJSONTyped(json, true);
        }
        if (json['type'] === 'STRING') {
            return StringPropertyModelFromJSONTyped(json, true);
        }
    }
    return {
        
        'advancedOption': !exists(json, 'advancedOption') ? undefined : json['advancedOption'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'displayCondition': !exists(json, 'displayCondition') ? undefined : json['displayCondition'],
        'expressionEnabled': !exists(json, 'expressionEnabled') ? undefined : json['expressionEnabled'],
        'hidden': !exists(json, 'hidden') ? undefined : json['hidden'],
        'label': !exists(json, 'label') ? undefined : json['label'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'placeholder': !exists(json, 'placeholder') ? undefined : json['placeholder'],
        'required': !exists(json, 'required') ? undefined : json['required'],
        'type': !exists(json, 'type') ? undefined : PropertyTypeModelFromJSON(json['type']),
    };
}

export function PropertyModelToJSON(value?: PropertyModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'advancedOption': value.advancedOption,
        'description': value.description,
        'displayCondition': value.displayCondition,
        'expressionEnabled': value.expressionEnabled,
        'hidden': value.hidden,
        'label': value.label,
        'name': value.name,
        'placeholder': value.placeholder,
        'required': value.required,
        'type': PropertyTypeModelToJSON(value.type),
    };
}

