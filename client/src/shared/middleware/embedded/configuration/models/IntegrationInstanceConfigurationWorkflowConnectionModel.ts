/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Configuration Internal API
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
 * The connection used in a particular task or trigger.
 * @export
 * @interface IntegrationInstanceConfigurationWorkflowConnectionModel
 */
export interface IntegrationInstanceConfigurationWorkflowConnectionModel {
    /**
     * The connection id
     * @type {number}
     * @memberof IntegrationInstanceConfigurationWorkflowConnectionModel
     */
    connectionId?: number;
    /**
     * The connection key under which a connection is defined in a workflow definition.
     * @type {string}
     * @memberof IntegrationInstanceConfigurationWorkflowConnectionModel
     */
    key?: string;
    /**
     * The action/trigger name to which a connection belongs.
     * @type {string}
     * @memberof IntegrationInstanceConfigurationWorkflowConnectionModel
     */
    workflowNodeName?: string;
}

/**
 * Check if a given object implements the IntegrationInstanceConfigurationWorkflowConnectionModel interface.
 */
export function instanceOfIntegrationInstanceConfigurationWorkflowConnectionModel(value: object): boolean {
    return true;
}

export function IntegrationInstanceConfigurationWorkflowConnectionModelFromJSON(json: any): IntegrationInstanceConfigurationWorkflowConnectionModel {
    return IntegrationInstanceConfigurationWorkflowConnectionModelFromJSONTyped(json, false);
}

export function IntegrationInstanceConfigurationWorkflowConnectionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): IntegrationInstanceConfigurationWorkflowConnectionModel {
    if (json == null) {
        return json;
    }
    return {
        
        'connectionId': json['connectionId'] == null ? undefined : json['connectionId'],
        'key': json['key'] == null ? undefined : json['key'],
        'workflowNodeName': json['workflowNodeName'] == null ? undefined : json['workflowNodeName'],
    };
}

export function IntegrationInstanceConfigurationWorkflowConnectionModelToJSON(value?: IntegrationInstanceConfigurationWorkflowConnectionModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'connectionId': value['connectionId'],
        'key': value['key'],
        'workflowNodeName': value['workflowNodeName'],
    };
}

