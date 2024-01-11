/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Configuration API
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
 * @interface WorkflowOutputModel
 */
export interface WorkflowOutputModel {
    /**
     * The name of an output
     * @type {string}
     * @memberof WorkflowOutputModel
     */
    name: string;
    /**
     * The value of an output
     * @type {object}
     * @memberof WorkflowOutputModel
     */
    value: object;
}

/**
 * Check if a given object implements the WorkflowOutputModel interface.
 */
export function instanceOfWorkflowOutputModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "value" in value;

    return isInstance;
}

export function WorkflowOutputModelFromJSON(json: any): WorkflowOutputModel {
    return WorkflowOutputModelFromJSONTyped(json, false);
}

export function WorkflowOutputModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowOutputModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'name': json['name'],
        'value': json['value'],
    };
}

export function WorkflowOutputModelToJSON(value?: WorkflowOutputModel | null): any {
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

