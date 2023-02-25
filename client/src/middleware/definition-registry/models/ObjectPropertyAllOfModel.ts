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
import type { PropertyModel } from './PropertyModel';
import {
    PropertyModelFromJSON,
    PropertyModelFromJSONTyped,
    PropertyModelToJSON,
} from './PropertyModel';

/**
 * 
 * @export
 * @interface ObjectPropertyAllOfModel
 */
export interface ObjectPropertyAllOfModel {
    /**
     * Types of dynamically defined properties.
     * @type {Array<PropertyModel>}
     * @memberof ObjectPropertyAllOfModel
     */
    additionalProperties?: Array<PropertyModel>;
    /**
     * If the object can contain multiple additional properties.
     * @type {boolean}
     * @memberof ObjectPropertyAllOfModel
     */
    multipleValues?: boolean;
    /**
     * The object type.
     * @type {string}
     * @memberof ObjectPropertyAllOfModel
     */
    objectType?: string;
    /**
     * The list of valid object property types.
     * @type {Array<PropertyModel>}
     * @memberof ObjectPropertyAllOfModel
     */
    properties?: Array<PropertyModel>;
}

/**
 * Check if a given object implements the ObjectPropertyAllOfModel interface.
 */
export function instanceOfObjectPropertyAllOfModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function ObjectPropertyAllOfModelFromJSON(json: any): ObjectPropertyAllOfModel {
    return ObjectPropertyAllOfModelFromJSONTyped(json, false);
}

export function ObjectPropertyAllOfModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ObjectPropertyAllOfModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'additionalProperties': !exists(json, 'additionalProperties') ? undefined : ((json['additionalProperties'] as Array<any>).map(PropertyModelFromJSON)),
        'multipleValues': !exists(json, 'multipleValues') ? undefined : json['multipleValues'],
        'objectType': !exists(json, 'objectType') ? undefined : json['objectType'],
        'properties': !exists(json, 'properties') ? undefined : ((json['properties'] as Array<any>).map(PropertyModelFromJSON)),
    };
}

export function ObjectPropertyAllOfModelToJSON(value?: ObjectPropertyAllOfModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'additionalProperties': value.additionalProperties === undefined ? undefined : ((value.additionalProperties as Array<any>).map(PropertyModelToJSON)),
        'multipleValues': value.multipleValues,
        'objectType': value.objectType,
        'properties': value.properties === undefined ? undefined : ((value.properties as Array<any>).map(PropertyModelToJSON)),
    };
}

