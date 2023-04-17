/* tslint:disable */
/* eslint-disable */
/**
 * Project API
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
 * @interface InputModel
 */
export interface InputModel {
    /**
     * The string of an input
     * @type {string}
     * @memberof InputModel
     */
    label?: string;
    /**
     * The name of an output
     * @type {string}
     * @memberof InputModel
     */
    name: string;
    /**
     * If an input is required, or not
     * @type {boolean}
     * @memberof InputModel
     */
    required?: boolean;
    /**
     * The type of an input, for example \"string\"
     * @type {string}
     * @memberof InputModel
     */
    type?: string;
}

/**
 * Check if a given object implements the InputModel interface.
 */
export function instanceOfInputModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function InputModelFromJSON(json: any): InputModel {
    return InputModelFromJSONTyped(json, false);
}

export function InputModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): InputModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'label': !exists(json, 'label') ? undefined : json['label'],
        'name': json['name'],
        'required': !exists(json, 'required') ? undefined : json['required'],
        'type': !exists(json, 'type') ? undefined : json['type'],
    };
}

export function InputModelToJSON(value?: InputModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'label': value.label,
        'name': value.name,
        'required': value.required,
        'type': value.type,
    };
}

