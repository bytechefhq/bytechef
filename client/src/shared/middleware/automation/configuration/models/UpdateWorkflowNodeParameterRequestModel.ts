/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Configuration API
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
 * @interface UpdateWorkflowNodeParameterRequestModel
 */
export interface UpdateWorkflowNodeParameterRequestModel {
    /**
     * The workflow node parameter path.
     * @type {string}
     * @memberof UpdateWorkflowNodeParameterRequestModel
     */
    path: string;
    /**
     * The value.
     * @type {object}
     * @memberof UpdateWorkflowNodeParameterRequestModel
     */
    value?: object;
    /**
     * The workflow node name.
     * @type {string}
     * @memberof UpdateWorkflowNodeParameterRequestModel
     */
    workflowNodeName: string;
}

/**
 * Check if a given object implements the UpdateWorkflowNodeParameterRequestModel interface.
 */
export function instanceOfUpdateWorkflowNodeParameterRequestModel(value: object): boolean {
    if (!('path' in value)) return false;
    if (!('workflowNodeName' in value)) return false;
    return true;
}

export function UpdateWorkflowNodeParameterRequestModelFromJSON(json: any): UpdateWorkflowNodeParameterRequestModel {
    return UpdateWorkflowNodeParameterRequestModelFromJSONTyped(json, false);
}

export function UpdateWorkflowNodeParameterRequestModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): UpdateWorkflowNodeParameterRequestModel {
    if (json == null) {
        return json;
    }
    return {
        
        'path': json['path'],
        'value': json['value'] == null ? undefined : json['value'],
        'workflowNodeName': json['workflowNodeName'],
    };
}

export function UpdateWorkflowNodeParameterRequestModelToJSON(value?: UpdateWorkflowNodeParameterRequestModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'path': value['path'],
        'value': value['value'],
        'workflowNodeName': value['workflowNodeName'],
    };
}

