/* tslint:disable */
/* eslint-disable */
/**
 * Core Workflow API
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
 * @interface WorkflowConnectionModel
 */
export interface WorkflowConnectionModel {
    /**
     * 
     * @type {string}
     * @memberof WorkflowConnectionModel
     */
    componentName: string;
    /**
     * 
     * @type {number}
     * @memberof WorkflowConnectionModel
     */
    componentVersion: number;
}

/**
 * Check if a given object implements the WorkflowConnectionModel interface.
 */
export function instanceOfWorkflowConnectionModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "componentName" in value;
    isInstance = isInstance && "componentVersion" in value;

    return isInstance;
}

export function WorkflowConnectionModelFromJSON(json: any): WorkflowConnectionModel {
    return WorkflowConnectionModelFromJSONTyped(json, false);
}

export function WorkflowConnectionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowConnectionModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'componentName': json['componentName'],
        'componentVersion': json['componentVersion'],
    };
}

export function WorkflowConnectionModelToJSON(value?: WorkflowConnectionModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'componentName': value.componentName,
        'componentVersion': value.componentVersion,
    };
}

