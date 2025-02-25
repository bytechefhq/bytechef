/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Internal API
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
 * @interface UpdateAiProviderRequest
 */
export interface UpdateAiProviderRequest {
    /**
     * The API key of an AI provider.
     * @type {string}
     * @memberof UpdateAiProviderRequest
     */
    apiKey?: string;
}

/**
 * Check if a given object implements the UpdateAiProviderRequest interface.
 */
export function instanceOfUpdateAiProviderRequest(value: object): value is UpdateAiProviderRequest {
    return true;
}

export function UpdateAiProviderRequestFromJSON(json: any): UpdateAiProviderRequest {
    return UpdateAiProviderRequestFromJSONTyped(json, false);
}

export function UpdateAiProviderRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): UpdateAiProviderRequest {
    if (json == null) {
        return json;
    }
    return {
        
        'apiKey': json['apiKey'] == null ? undefined : json['apiKey'],
    };
}

export function UpdateAiProviderRequestToJSON(json: any): UpdateAiProviderRequest {
    return UpdateAiProviderRequestToJSONTyped(json, false);
}

export function UpdateAiProviderRequestToJSONTyped(value?: UpdateAiProviderRequest | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'apiKey': value['apiKey'],
    };
}

