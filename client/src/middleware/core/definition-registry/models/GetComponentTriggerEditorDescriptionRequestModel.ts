/* tslint:disable */
/* eslint-disable */
/**
 * Core Definition API
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
 * @interface GetComponentTriggerEditorDescriptionRequestModel
 */
export interface GetComponentTriggerEditorDescriptionRequestModel {
    /**
     * The connection id.
     * @type {number}
     * @memberof GetComponentTriggerEditorDescriptionRequestModel
     */
    connectionId: number;
    /**
     * The parameters of an trigger.
     * @type {{ [key: string]: object; }}
     * @memberof GetComponentTriggerEditorDescriptionRequestModel
     */
    parameters: { [key: string]: object; };
}

/**
 * Check if a given object implements the GetComponentTriggerEditorDescriptionRequestModel interface.
 */
export function instanceOfGetComponentTriggerEditorDescriptionRequestModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "connectionId" in value;
    isInstance = isInstance && "parameters" in value;

    return isInstance;
}

export function GetComponentTriggerEditorDescriptionRequestModelFromJSON(json: any): GetComponentTriggerEditorDescriptionRequestModel {
    return GetComponentTriggerEditorDescriptionRequestModelFromJSONTyped(json, false);
}

export function GetComponentTriggerEditorDescriptionRequestModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetComponentTriggerEditorDescriptionRequestModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'connectionId': json['connectionId'],
        'parameters': json['parameters'],
    };
}

export function GetComponentTriggerEditorDescriptionRequestModelToJSON(value?: GetComponentTriggerEditorDescriptionRequestModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'connectionId': value.connectionId,
        'parameters': value.parameters,
    };
}

