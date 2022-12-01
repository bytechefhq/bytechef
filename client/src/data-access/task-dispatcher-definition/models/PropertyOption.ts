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

/**
 * Defines valid property value.
 * @export
 * @interface PropertyOption
 */
export interface PropertyOption {
    /**
     * Description of the option.
     * @type {string}
     * @memberof PropertyOption
     */
    description?: string;
    /**
     * 
     * @type {DisplayOption}
     * @memberof PropertyOption
     */
    displayOption?: DisplayOption;
    /**
     * Name of the option.
     * @type {string}
     * @memberof PropertyOption
     */
    name?: string;
    /**
     * Value of the option.
     * @type {object}
     * @memberof PropertyOption
     */
    value?: object;
}

/**
 * Check if a given object implements the PropertyOption interface.
 */
export function instanceOfPropertyOption(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PropertyOptionFromJSON(json: any): PropertyOption {
    return PropertyOptionFromJSONTyped(json, false);
}

export function PropertyOptionFromJSONTyped(json: any, ignoreDiscriminator: boolean): PropertyOption {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'description': !exists(json, 'description') ? undefined : json['description'],
        'displayOption': !exists(json, 'displayOption') ? undefined : DisplayOptionFromJSON(json['displayOption']),
        'name': !exists(json, 'name') ? undefined : json['name'],
        'value': !exists(json, 'value') ? undefined : json['value'],
    };
}

export function PropertyOptionToJSON(value?: PropertyOption | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'description': value.description,
        'displayOption': DisplayOptionToJSON(value.displayOption),
        'name': value.name,
        'value': value.value,
    };
}

