/* tslint:disable */
/* eslint-disable */
/**
 * Integration API
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
/**
 * 
 * @export
 * @interface OutputModel
 */
export interface OutputModel {
    /**
     * The name of an output
     * @type {string}
     * @memberof OutputModel
     */
    name: string;
    /**
     * The value of an output
     * @type {object}
     * @memberof OutputModel
     */
    value: object;
}

/**
 * Check if a given object implements the OutputModel interface.
 */
export function instanceOfOutputModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "value" in value;

    return isInstance;
}

export function OutputModelFromJSON(json: any): OutputModel {
    return OutputModelFromJSONTyped(json, false);
}

export function OutputModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): OutputModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'name': json['name'],
        'value': json['value'],
    };
}

export function OutputModelToJSON(value?: OutputModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'name': value.name,
        'value': value.value,
    };
}

