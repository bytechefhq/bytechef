/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration Internal API
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
 * @interface TaskDispatcherOperationRequest
 */
export interface TaskDispatcherOperationRequest {
    /**
     * The parameters of an action.
     * @type {{ [key: string]: any; }}
     * @memberof TaskDispatcherOperationRequest
     */
    parameters: { [key: string]: any; };
}

/**
 * Check if a given object implements the TaskDispatcherOperationRequest interface.
 */
export function instanceOfTaskDispatcherOperationRequest(value: object): value is TaskDispatcherOperationRequest {
    if (!('parameters' in value) || value['parameters'] === undefined) return false;
    return true;
}

export function TaskDispatcherOperationRequestFromJSON(json: any): TaskDispatcherOperationRequest {
    return TaskDispatcherOperationRequestFromJSONTyped(json, false);
}

export function TaskDispatcherOperationRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): TaskDispatcherOperationRequest {
    if (json == null) {
        return json;
    }
    return {
        
        'parameters': json['parameters'],
    };
}

export function TaskDispatcherOperationRequestToJSON(json: any): TaskDispatcherOperationRequest {
    return TaskDispatcherOperationRequestToJSONTyped(json, false);
}

export function TaskDispatcherOperationRequestToJSONTyped(value?: TaskDispatcherOperationRequest | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'parameters': value['parameters'],
    };
}

