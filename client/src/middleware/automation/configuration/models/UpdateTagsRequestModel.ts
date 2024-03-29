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
import type { TagModel } from './TagModel';
import {
    TagModelFromJSON,
    TagModelFromJSONTyped,
    TagModelToJSON,
} from './TagModel';

/**
 * The request object that contains the array of tags.
 * @export
 * @interface UpdateTagsRequestModel
 */
export interface UpdateTagsRequestModel {
    /**
     * 
     * @type {Array<TagModel>}
     * @memberof UpdateTagsRequestModel
     */
    tags?: Array<TagModel>;
}

/**
 * Check if a given object implements the UpdateTagsRequestModel interface.
 */
export function instanceOfUpdateTagsRequestModel(value: object): boolean {
    return true;
}

export function UpdateTagsRequestModelFromJSON(json: any): UpdateTagsRequestModel {
    return UpdateTagsRequestModelFromJSONTyped(json, false);
}

export function UpdateTagsRequestModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): UpdateTagsRequestModel {
    if (json == null) {
        return json;
    }
    return {
        
        'tags': json['tags'] == null ? undefined : ((json['tags'] as Array<any>).map(TagModelFromJSON)),
    };
}

export function UpdateTagsRequestModelToJSON(value?: UpdateTagsRequestModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'tags': value['tags'] == null ? undefined : ((value['tags'] as Array<any>).map(TagModelToJSON)),
    };
}

