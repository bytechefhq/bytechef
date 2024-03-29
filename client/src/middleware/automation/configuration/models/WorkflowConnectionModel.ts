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
 * @interface WorkflowConnectionModel
 */
export interface WorkflowConnectionModel {
    /**
     * The name of the component
     * @type {string}
     * @memberof WorkflowConnectionModel
     */
    componentName: string;
    /**
     * The version of the component
     * @type {number}
     * @memberof WorkflowConnectionModel
     */
    componentVersion: number;
    /**
     * The key of the connection
     * @type {string}
     * @memberof WorkflowConnectionModel
     */
    key: string;
    /**
     * If the connection is required, or not
     * @type {boolean}
     * @memberof WorkflowConnectionModel
     */
    required: boolean;
    /**
     * 
     * @type {string}
     * @memberof WorkflowConnectionModel
     */
    workflowNodeName: string;
}

/**
 * Check if a given object implements the WorkflowConnectionModel interface.
 */
export function instanceOfWorkflowConnectionModel(value: object): boolean {
    if (!('componentName' in value)) return false;
    if (!('componentVersion' in value)) return false;
    if (!('key' in value)) return false;
    if (!('required' in value)) return false;
    if (!('workflowNodeName' in value)) return false;
    return true;
}

export function WorkflowConnectionModelFromJSON(json: any): WorkflowConnectionModel {
    return WorkflowConnectionModelFromJSONTyped(json, false);
}

export function WorkflowConnectionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowConnectionModel {
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

export function WorkflowConnectionModelToJSON(value?: WorkflowConnectionModel | null): any {
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

