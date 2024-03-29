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
 * 
 * @export
 * @interface SaveWorkflowTestConfigurationConnectionRequestModel
 */
export interface SaveWorkflowTestConfigurationConnectionRequestModel {
    /**
     * 
     * @type {number}
     * @memberof SaveWorkflowTestConfigurationConnectionRequestModel
     */
    connectionId?: number;
}

/**
 * Check if a given object implements the SaveWorkflowTestConfigurationConnectionRequestModel interface.
 */
export function instanceOfSaveWorkflowTestConfigurationConnectionRequestModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function SaveWorkflowTestConfigurationConnectionRequestModelFromJSON(json: any): SaveWorkflowTestConfigurationConnectionRequestModel {
    return SaveWorkflowTestConfigurationConnectionRequestModelFromJSONTyped(json, false);
}

export function SaveWorkflowTestConfigurationConnectionRequestModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): SaveWorkflowTestConfigurationConnectionRequestModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'connectionId': !exists(json, 'connectionId') ? undefined : json['connectionId'],
    };
}

export function SaveWorkflowTestConfigurationConnectionRequestModelToJSON(value?: SaveWorkflowTestConfigurationConnectionRequestModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'connectionId': value.connectionId,
    };
}

