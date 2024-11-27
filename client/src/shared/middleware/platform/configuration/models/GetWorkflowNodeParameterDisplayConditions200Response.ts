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
 * @interface GetWorkflowNodeParameterDisplayConditions200Response
 */
export interface GetWorkflowNodeParameterDisplayConditions200Response {
    /**
     * 
     * @type {{ [key: string]: boolean; }}
     * @memberof GetWorkflowNodeParameterDisplayConditions200Response
     */
    displayConditions?: { [key: string]: boolean; };
}

/**
 * Check if a given object implements the GetWorkflowNodeParameterDisplayConditions200Response interface.
 */
export function instanceOfGetWorkflowNodeParameterDisplayConditions200Response(value: object): value is GetWorkflowNodeParameterDisplayConditions200Response {
    return true;
}

export function GetWorkflowNodeParameterDisplayConditions200ResponseFromJSON(json: any): GetWorkflowNodeParameterDisplayConditions200Response {
    return GetWorkflowNodeParameterDisplayConditions200ResponseFromJSONTyped(json, false);
}

export function GetWorkflowNodeParameterDisplayConditions200ResponseFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetWorkflowNodeParameterDisplayConditions200Response {
    if (json == null) {
        return json;
    }
    return {
        
        'displayConditions': json['displayConditions'] == null ? undefined : json['displayConditions'],
    };
}

export function GetWorkflowNodeParameterDisplayConditions200ResponseToJSON(json: any): GetWorkflowNodeParameterDisplayConditions200Response {
    return GetWorkflowNodeParameterDisplayConditions200ResponseToJSONTyped(json, false);
}

export function GetWorkflowNodeParameterDisplayConditions200ResponseToJSONTyped(value?: GetWorkflowNodeParameterDisplayConditions200Response | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'displayConditions': value['displayConditions'],
    };
}

