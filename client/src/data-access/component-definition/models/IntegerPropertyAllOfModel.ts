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
/**
 * 
 * @export
 * @interface IntegerPropertyAllOfModel
 */
export interface IntegerPropertyAllOfModel {
    /**
     * The maximum property value.
     * @type {number}
     * @memberof IntegerPropertyAllOfModel
     */
    maxValue?: number;
    /**
     * The minimum property value.
     * @type {number}
     * @memberof IntegerPropertyAllOfModel
     */
    minValue?: number;
    /**
     * 
     * @type {string}
     * @memberof IntegerPropertyAllOfModel
     */
    type?: string;
}

/**
 * Check if a given object implements the IntegerPropertyAllOfModel interface.
 */
export function instanceOfIntegerPropertyAllOfModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function IntegerPropertyAllOfModelFromJSON(json: any): IntegerPropertyAllOfModel {
    return IntegerPropertyAllOfModelFromJSONTyped(json, false);
}

export function IntegerPropertyAllOfModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): IntegerPropertyAllOfModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'maxValue': !exists(json, 'maxValue') ? undefined : json['maxValue'],
        'minValue': !exists(json, 'minValue') ? undefined : json['minValue'],
        'type': !exists(json, 'type') ? undefined : json['type'],
    };
}

export function IntegerPropertyAllOfModelToJSON(value?: IntegerPropertyAllOfModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'maxValue': value.maxValue,
        'minValue': value.minValue,
        'type': value.type,
    };
}

