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
 * @interface OneOfPropertyAllOfModel
 */
export interface OneOfPropertyAllOfModel {
    /**
     * 
     * @type {boolean}
     * @memberof OneOfPropertyAllOfModel
     */
    required?: boolean;
    /**
     * Possible types of properties that can be used.
     * @type {Array<PropertyModel>}
     * @memberof OneOfPropertyAllOfModel
     */
    types?: Array<PropertyModel>;
    /**
     * 
     * @type {string}
     * @memberof OneOfPropertyAllOfModel
     */
    type?: string;
}

/**
 * Check if a given object implements the OneOfPropertyAllOfModel interface.
 */
export function instanceOfOneOfPropertyAllOfModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function OneOfPropertyAllOfModelFromJSON(json: any): OneOfPropertyAllOfModel {
    return OneOfPropertyAllOfModelFromJSONTyped(json, false);
}

export function OneOfPropertyAllOfModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): OneOfPropertyAllOfModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'required': !exists(json, 'required') ? undefined : json['required'],
        'types': !exists(json, 'types') ? undefined : ((json['types'] as Array<any>).map(PropertyModelFromJSON)),
        'type': !exists(json, 'type') ? undefined : json['type'],
    };
}

export function OneOfPropertyAllOfModelToJSON(value?: OneOfPropertyAllOfModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'required': value.required,
        'types': value.types === undefined ? undefined : ((value.types as Array<any>).map(PropertyModelToJSON)),
        'type': value.type,
    };
}

