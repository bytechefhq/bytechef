/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Category Internal API
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
 * A category.
 * @export
 * @interface Category
 */
export interface Category {
    /**
     * The created by.
     * @type {string}
     * @memberof Category
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof Category
     */
    readonly createdDate?: Date;
    /**
     * The id of the category.
     * @type {number}
     * @memberof Category
     */
    id?: number;
    /**
     * The name of the category.
     * @type {string}
     * @memberof Category
     */
    name: string;
    /**
     * The last modified by.
     * @type {string}
     * @memberof Category
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof Category
     */
    readonly lastModifiedDate?: Date;
    /**
     * 
     * @type {number}
     * @memberof Category
     */
    version?: number;
}

/**
 * Check if a given object implements the Category interface.
 */
export function instanceOfCategory(value: object): value is Category {
    if (!('name' in value) || value['name'] === undefined) return false;
    return true;
}

export function CategoryFromJSON(json: any): Category {
    return CategoryFromJSONTyped(json, false);
}

export function CategoryFromJSONTyped(json: any, ignoreDiscriminator: boolean): Category {
    if (json == null) {
        return json;
    }
    return {
        
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'id': json['id'] == null ? undefined : json['id'],
        'name': json['name'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'version': json['__version'] == null ? undefined : json['__version'],
    };
}

export function CategoryToJSON(json: any): Category {
    return CategoryToJSONTyped(json, false);
}

export function CategoryToJSONTyped(value?: Omit<Category, 'createdBy'|'createdDate'|'lastModifiedBy'|'lastModifiedDate'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'id': value['id'],
        'name': value['name'],
        '__version': value['version'],
    };
}

