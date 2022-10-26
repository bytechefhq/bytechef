/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { DisplayOption } from './DisplayOption';
import {
    DisplayOptionFromJSON,
    DisplayOptionFromJSONTyped,
    DisplayOptionToJSON,
} from './DisplayOption';
import type { PropertyOption } from './PropertyOption';
import {
    PropertyOptionFromJSON,
    PropertyOptionFromJSONTyped,
    PropertyOptionToJSON,
} from './PropertyOption';
import type { PropertyType } from './PropertyType';
import {
    PropertyTypeFromJSON,
    PropertyTypeFromJSONTyped,
    PropertyTypeToJSON,
} from './PropertyType';

/**
 * 
 * @export
 * @interface PrimitiveValueProperty
 */
export interface PrimitiveValueProperty {
    /**
     * 
     * @type {Array<PropertyOption>}
     * @memberof PrimitiveValueProperty
     */
    options?: Array<PropertyOption>;
    /**
     * 
     * @type {string}
     * @memberof PrimitiveValueProperty
     */
    description?: string;
    /**
     * 
     * @type {DisplayOption}
     * @memberof PrimitiveValueProperty
     */
    displayOption?: DisplayOption;
    /**
     * 
     * @type {string}
     * @memberof PrimitiveValueProperty
     */
    label?: string;
    /**
     * 
     * @type {string}
     * @memberof PrimitiveValueProperty
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof PrimitiveValueProperty
     */
    placeholder?: string;
    /**
     * 
     * @type {PropertyType}
     * @memberof PrimitiveValueProperty
     */
    type?: PropertyType;
}

/**
 * Check if a given object implements the PrimitiveValueProperty interface.
 */
export function instanceOfPrimitiveValueProperty(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PrimitiveValuePropertyFromJSON(json: any): PrimitiveValueProperty {
    return PrimitiveValuePropertyFromJSONTyped(json, false);
}

export function PrimitiveValuePropertyFromJSONTyped(json: any, ignoreDiscriminator: boolean): PrimitiveValueProperty {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'options': !exists(json, 'options') ? undefined : ((json['options'] as Array<any>).map(PropertyOptionFromJSON)),
        'description': !exists(json, 'description') ? undefined : json['description'],
        'displayOption': !exists(json, 'displayOption') ? undefined : DisplayOptionFromJSON(json['displayOption']),
        'label': !exists(json, 'label') ? undefined : json['label'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'placeholder': !exists(json, 'placeholder') ? undefined : json['placeholder'],
        'type': !exists(json, 'type') ? undefined : PropertyTypeFromJSON(json['type']),
    };
}

export function PrimitiveValuePropertyToJSON(value?: PrimitiveValueProperty | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'options': value.options === undefined ? undefined : ((value.options as Array<any>).map(PropertyOptionToJSON)),
        'description': value.description,
        'displayOption': DisplayOptionToJSON(value.displayOption),
        'label': value.label,
        'name': value.name,
        'placeholder': value.placeholder,
        'type': PropertyTypeToJSON(value.type),
    };
}

