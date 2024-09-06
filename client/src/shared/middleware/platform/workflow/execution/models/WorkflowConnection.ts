/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Workflow Execution Internal API
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
 * @interface WorkflowConnection
 */
export interface WorkflowConnection {
    /**
     * The name of the component
     * @type {string}
     * @memberof WorkflowConnection
     */
    componentName: string;
    /**
     * The version of the component
     * @type {number}
     * @memberof WorkflowConnection
     */
    componentVersion: number;
    /**
     * The key of the connection
     * @type {string}
     * @memberof WorkflowConnection
     */
    key: string;
    /**
     * If the connection is required, or not
     * @type {boolean}
     * @memberof WorkflowConnection
     */
    required: boolean;
    /**
     * 
     * @type {string}
     * @memberof WorkflowConnection
     */
    workflowNodeName: string;
}

/**
 * Check if a given object implements the WorkflowConnection interface.
 */
export function instanceOfWorkflowConnection(value: object): value is WorkflowConnection {
    if (!('componentName' in value) || value['componentName'] === undefined) return false;
    if (!('componentVersion' in value) || value['componentVersion'] === undefined) return false;
    if (!('key' in value) || value['key'] === undefined) return false;
    if (!('required' in value) || value['required'] === undefined) return false;
    if (!('workflowNodeName' in value) || value['workflowNodeName'] === undefined) return false;
    return true;
}

export function WorkflowConnectionFromJSON(json: any): WorkflowConnection {
    return WorkflowConnectionFromJSONTyped(json, false);
}

export function WorkflowConnectionFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowConnection {
    if (json == null) {
        return json;
    }
    return {
        
        'componentName': json['componentName'],
        'componentVersion': json['componentVersion'],
        'key': json['key'],
        'required': json['required'],
        'workflowNodeName': json['workflowNodeName'],
    };
}

export function WorkflowConnectionToJSON(value?: WorkflowConnection | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'componentName': value['componentName'],
        'componentVersion': value['componentVersion'],
        'key': value['key'],
        'required': value['required'],
        'workflowNodeName': value['workflowNodeName'],
    };
}
