/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Configuration Internal API
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
/**
 * 
 * @export
 * @interface WorkflowInput
 */
export interface WorkflowInput {
    /**
     * The descriptive name of an input
     * @type {string}
     * @memberof WorkflowInput
     */
    label?: string;
    /**
     * The name of an input
     * @type {string}
     * @memberof WorkflowInput
     */
    name: string;
    /**
     * If an input is required, or not
     * @type {boolean}
     * @memberof WorkflowInput
     */
    required?: boolean;
    /**
     * The type of an input, for example \"string\"
     * @type {string}
     * @memberof WorkflowInput
     */
    type?: string;
}

/**
 * Check if a given object implements the WorkflowInput interface.
 */
export function instanceOfWorkflowInput(value: object): value is WorkflowInput {
    if (!('name' in value) || value['name'] === undefined) return false;
    return true;
}

export function WorkflowInputFromJSON(json: any): WorkflowInput {
    return WorkflowInputFromJSONTyped(json, false);
}

export function WorkflowInputFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowInput {
    if (json == null) {
        return json;
    }
    return {
        
        'label': json['label'] == null ? undefined : json['label'],
        'name': json['name'],
        'required': json['required'] == null ? undefined : json['required'],
        'type': json['type'] == null ? undefined : json['type'],
    };
}

export function WorkflowInputToJSON(json: any): WorkflowInput {
    return WorkflowInputToJSONTyped(json, false);
}

export function WorkflowInputToJSONTyped(value?: WorkflowInput | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'label': value['label'],
        'name': value['name'],
        'required': value['required'],
        'type': value['type'],
    };
}

