/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Connection Internal API
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
 * A tag.
 * @export
 * @interface TagModel
 */
export interface TagModel {
    /**
     * The created by.
     * @type {string}
     * @memberof TagModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof TagModel
     */
    readonly createdDate?: Date;
    /**
     * The id of the tag.
     * @type {number}
     * @memberof TagModel
     */
    id?: number;
    /**
     * The last modified by.
     * @type {string}
     * @memberof TagModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof TagModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The name of the tag.
     * @type {string}
     * @memberof TagModel
     */
    name: string;
    /**
     * 
     * @type {number}
     * @memberof TagModel
     */
    version?: number;
}

/**
 * Check if a given object implements the TagModel interface.
 */
export function instanceOfTagModel(value: object): boolean {
    if (!('name' in value)) return false;
    return true;
}

export function TagModelFromJSON(json: any): TagModel {
    return TagModelFromJSONTyped(json, false);
}

export function TagModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): TagModel {
    if (json == null) {
        return json;
    }
    return {
        
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'id': json['id'] == null ? undefined : json['id'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'name': json['name'],
        'version': json['__version'] == null ? undefined : json['__version'],
    };
}

export function TagModelToJSON(value?: Omit<TagModel, 'createdBy'|'createdDate'|'lastModifiedBy'|'lastModifiedDate'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'id': value['id'],
        'name': value['name'],
        '__version': value['version'],
    };
}

