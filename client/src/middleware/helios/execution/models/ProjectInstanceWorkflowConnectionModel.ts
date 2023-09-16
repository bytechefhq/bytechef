/* tslint:disable */
/* eslint-disable */
/**
 * Project Execution API
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
 * The connection used in a particular task.
 * @export
 * @interface ProjectInstanceWorkflowConnectionModel
 */
export interface ProjectInstanceWorkflowConnectionModel {
    /**
     * The connection id
     * @type {number}
     * @memberof ProjectInstanceWorkflowConnectionModel
     */
    connectionId?: number;
    /**
     * The connection key under which a connection is defined in a workflow definition.
     * @type {string}
     * @memberof ProjectInstanceWorkflowConnectionModel
     */
    key?: string;
    /**
     * The action/trigger name to which a connection belongs.
     * @type {string}
     * @memberof ProjectInstanceWorkflowConnectionModel
     */
    operationName?: string;
}

/**
 * Check if a given object implements the ProjectInstanceWorkflowConnectionModel interface.
 */
export function instanceOfProjectInstanceWorkflowConnectionModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function ProjectInstanceWorkflowConnectionModelFromJSON(json: any): ProjectInstanceWorkflowConnectionModel {
    return ProjectInstanceWorkflowConnectionModelFromJSONTyped(json, false);
}

export function ProjectInstanceWorkflowConnectionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ProjectInstanceWorkflowConnectionModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'connectionId': !exists(json, 'connectionId') ? undefined : json['connectionId'],
        'key': !exists(json, 'key') ? undefined : json['key'],
        'operationName': !exists(json, 'operationName') ? undefined : json['operationName'],
    };
}

export function ProjectInstanceWorkflowConnectionModelToJSON(value?: ProjectInstanceWorkflowConnectionModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'connectionId': value.connectionId,
        'key': value.key,
        'operationName': value.operationName,
    };
}

