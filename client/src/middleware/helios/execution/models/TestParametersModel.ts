/* tslint:disable */
/* eslint-disable */
/**
 * Automation Execution API
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
import type { TaskConnectionModel } from './TaskConnectionModel';
import {
    TaskConnectionModelFromJSON,
    TaskConnectionModelFromJSONTyped,
    TaskConnectionModelToJSON,
} from './TaskConnectionModel';

/**
 * Defines parameters used to test a workflow.
 * @export
 * @interface TestParametersModel
 */
export interface TestParametersModel {
    /**
     * 
     * @type {Array<TaskConnectionModel>}
     * @memberof TestParametersModel
     */
    connections?: Array<TaskConnectionModel>;
    /**
     * The inputs expected by the workflow
     * @type {{ [key: string]: object; }}
     * @memberof TestParametersModel
     */
    inputs?: { [key: string]: object; };
    /**
     * Id of the workflow to execute.
     * @type {string}
     * @memberof TestParametersModel
     */
    workflowId?: string;
}

/**
 * Check if a given object implements the TestParametersModel interface.
 */
export function instanceOfTestParametersModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function TestParametersModelFromJSON(json: any): TestParametersModel {
    return TestParametersModelFromJSONTyped(json, false);
}

export function TestParametersModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): TestParametersModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'connections': !exists(json, 'connections') ? undefined : ((json['connections'] as Array<any>).map(TaskConnectionModelFromJSON)),
        'inputs': !exists(json, 'inputs') ? undefined : json['inputs'],
        'workflowId': !exists(json, 'workflowId') ? undefined : json['workflowId'],
    };
}

export function TestParametersModelToJSON(value?: TestParametersModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'connections': value.connections === undefined ? undefined : ((value.connections as Array<any>).map(TaskConnectionModelToJSON)),
        'inputs': value.inputs,
        'workflowId': value.workflowId,
    };
}

