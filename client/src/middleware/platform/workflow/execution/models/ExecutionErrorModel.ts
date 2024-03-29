/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Workflow Execution API
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
 * Contains information about an error that happened during execution.
 * @export
 * @interface ExecutionErrorModel
 */
export interface ExecutionErrorModel {
    /**
     * The error message.
     * @type {string}
     * @memberof ExecutionErrorModel
     */
    message?: string;
    /**
     * The error stacktrace.
     * @type {Array<string>}
     * @memberof ExecutionErrorModel
     */
    stackTrace?: Array<string>;
}

/**
 * Check if a given object implements the ExecutionErrorModel interface.
 */
export function instanceOfExecutionErrorModel(value: object): boolean {
    return true;
}

export function ExecutionErrorModelFromJSON(json: any): ExecutionErrorModel {
    return ExecutionErrorModelFromJSONTyped(json, false);
}

export function ExecutionErrorModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ExecutionErrorModel {
    if (json == null) {
        return json;
    }
    return {
        
        'message': json['message'] == null ? undefined : json['message'],
        'stackTrace': json['stackTrace'] == null ? undefined : json['stackTrace'],
    };
}

export function ExecutionErrorModelToJSON(value?: ExecutionErrorModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'message': value['message'],
        'stackTrace': value['stackTrace'],
    };
}

