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
import type { PropertyOption } from './PropertyOption';
import {
    PropertyOptionFromJSON,
    PropertyOptionFromJSONTyped,
    PropertyOptionToJSON,
} from './PropertyOption';
import type { SingleValueProperty } from './SingleValueProperty';
import {
    SingleValuePropertyFromJSON,
    SingleValuePropertyFromJSONTyped,
    SingleValuePropertyToJSON,
} from './SingleValueProperty';

/**
 * A date property type.
 * @export
 * @interface DateProperty
 */
export interface DateProperty extends SingleValueProperty {
    /**
     * 
     * @type {string}
     * @memberof DateProperty
     */
    type: string;
}

/**
 * Check if a given object implements the DateProperty interface.
 */
export function instanceOfDateProperty(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "type" in value;

    return isInstance;
}

export function DatePropertyFromJSON(json: any): DateProperty {
    return DatePropertyFromJSONTyped(json, false);
}

export function DatePropertyFromJSONTyped(json: any, ignoreDiscriminator: boolean): DateProperty {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        ...SingleValuePropertyFromJSONTyped(json, ignoreDiscriminator),
        'type': json['type'],
    };
}

export function DatePropertyToJSON(value?: DateProperty | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        ...SingleValuePropertyToJSON(value),
        'type': value.type,
    };
}

