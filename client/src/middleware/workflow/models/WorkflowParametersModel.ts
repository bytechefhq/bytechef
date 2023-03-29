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
 * Defines parameters used to execute a workflow.
 * @export
 * @interface WorkflowParametersModel
 */
export interface WorkflowParametersModel {
    /**
     * The inputs expected by the workflow
     * @type {{ [key: string]: object; }}
     * @memberof WorkflowParametersModel
     */
    inputs?: { [key: string]: object; };
    /**
     * The outputs expected by the workflow.
     * @type {{ [key: string]: object; }}
     * @memberof WorkflowParametersModel
     */
    outputs?: { [key: string]: object; };
}

/**
 * Check if a given object implements the WorkflowParametersModel interface.
 */
export function instanceOfWorkflowParametersModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function WorkflowParametersModelFromJSON(json: any): WorkflowParametersModel {
    return WorkflowParametersModelFromJSONTyped(json, false);
}

export function WorkflowParametersModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowParametersModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'inputs': !exists(json, 'inputs') ? undefined : json['inputs'],
        'outputs': !exists(json, 'outputs') ? undefined : json['outputs'],
    };
}

export function WorkflowParametersModelToJSON(value?: WorkflowParametersModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'inputs': value.inputs,
        'outputs': value.outputs,
    };
}

