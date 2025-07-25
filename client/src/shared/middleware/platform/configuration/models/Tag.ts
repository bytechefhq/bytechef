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
 * A tag.
 * @export
 * @interface Tag
 */
export interface Tag {
    /**
     * The created by.
     * @type {string}
     * @memberof Tag
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof Tag
     */
    readonly createdDate?: Date;
    /**
     * The id of the tag.
     * @type {number}
     * @memberof Tag
     */
    id?: number;
    /**
     * The last modified by.
     * @type {string}
     * @memberof Tag
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof Tag
     */
    readonly lastModifiedDate?: Date;
    /**
     * The name of the tag.
     * @type {string}
     * @memberof Tag
     */
    name: string;
    /**
     * 
     * @type {number}
     * @memberof Tag
     */
    version?: number;
}

/**
 * Check if a given object implements the Tag interface.
 */
export function instanceOfTag(value: object): value is Tag {
    if (!('name' in value) || value['name'] === undefined) return false;
    return true;
}

export function TagFromJSON(json: any): Tag {
    return TagFromJSONTyped(json, false);
}

export function TagFromJSONTyped(json: any, ignoreDiscriminator: boolean): Tag {
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

export function TagToJSON(json: any): Tag {
    return TagToJSONTyped(json, false);
}

export function TagToJSONTyped(value?: Omit<Tag, 'createdBy'|'createdDate'|'lastModifiedBy'|'lastModifiedDate'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'id': value['id'],
        'name': value['name'],
        '__version': value['version'],
    };
}

