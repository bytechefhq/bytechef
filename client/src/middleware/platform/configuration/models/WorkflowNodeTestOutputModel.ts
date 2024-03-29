/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration API
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
 * Contains test output of a workflow node.
 * @export
 * @interface WorkflowNodeTestOutputModel
 */
export interface WorkflowNodeTestOutputModel {
    /**
     * The workflow test node output id
     * @type {number}
     * @memberof WorkflowNodeTestOutputModel
     */
    id?: number;
    /**
     * The workflow node name.
     * @type {string}
     * @memberof WorkflowNodeTestOutputModel
     */
    workflowNodeName?: string;
    /**
     * The workflow id.
     * @type {string}
     * @memberof WorkflowNodeTestOutputModel
     */
    workflowId?: string;
}

/**
 * Check if a given object implements the WorkflowNodeTestOutputModel interface.
 */
export function instanceOfWorkflowNodeTestOutputModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function WorkflowNodeTestOutputModelFromJSON(json: any): WorkflowNodeTestOutputModel {
    return WorkflowNodeTestOutputModelFromJSONTyped(json, false);
}

export function WorkflowNodeTestOutputModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowNodeTestOutputModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': !exists(json, 'id') ? undefined : json['id'],
        'workflowNodeName': !exists(json, 'workflowNodeName') ? undefined : json['workflowNodeName'],
        'workflowId': !exists(json, 'workflowId') ? undefined : json['workflowId'],
    };
}

export function WorkflowNodeTestOutputModelToJSON(value?: WorkflowNodeTestOutputModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'workflowNodeName': value.workflowNodeName,
        'workflowId': value.workflowId,
    };
}

