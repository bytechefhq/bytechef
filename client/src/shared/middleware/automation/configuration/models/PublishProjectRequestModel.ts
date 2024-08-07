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
 * @interface PublishProjectRequestModel
 */
export interface PublishProjectRequestModel {
    /**
     * The description of a project version.
     * @type {string}
     * @memberof PublishProjectRequestModel
     */
    description?: string;
}

/**
 * Check if a given object implements the PublishProjectRequestModel interface.
 */
export function instanceOfPublishProjectRequestModel(value: object): boolean {
    return true;
}

export function PublishProjectRequestModelFromJSON(json: any): PublishProjectRequestModel {
    return PublishProjectRequestModelFromJSONTyped(json, false);
}

export function PublishProjectRequestModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): PublishProjectRequestModel {
    if (json == null) {
        return json;
    }
    return {
        
        'description': json['description'] == null ? undefined : json['description'],
    };
}

export function PublishProjectRequestModelToJSON(value?: PublishProjectRequestModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'description': value['description'],
    };
}

