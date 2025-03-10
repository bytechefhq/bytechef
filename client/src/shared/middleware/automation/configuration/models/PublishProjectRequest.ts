/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Configuration Internal API
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
 * @interface PublishProjectRequest
 */
export interface PublishProjectRequest {
    /**
     * The description of a project version.
     * @type {string}
     * @memberof PublishProjectRequest
     */
    description?: string;
}

/**
 * Check if a given object implements the PublishProjectRequest interface.
 */
export function instanceOfPublishProjectRequest(value: object): value is PublishProjectRequest {
    return true;
}

export function PublishProjectRequestFromJSON(json: any): PublishProjectRequest {
    return PublishProjectRequestFromJSONTyped(json, false);
}

export function PublishProjectRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): PublishProjectRequest {
    if (json == null) {
        return json;
    }
    return {
        
        'description': json['description'] == null ? undefined : json['description'],
    };
}

export function PublishProjectRequestToJSON(json: any): PublishProjectRequest {
    return PublishProjectRequestToJSONTyped(json, false);
}

export function PublishProjectRequestToJSONTyped(value?: PublishProjectRequest | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'description': value['description'],
    };
}

