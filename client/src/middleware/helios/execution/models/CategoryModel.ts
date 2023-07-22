/* tslint:disable */
/* eslint-disable */
/**
 * Automation Execution API
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
 * A category.
 * @export
 * @interface CategoryModel
 */
export interface CategoryModel {
    /**
     * The created by.
     * @type {string}
     * @memberof CategoryModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof CategoryModel
     */
    readonly createdDate?: Date;
    /**
     * The id of the category.
     * @type {number}
     * @memberof CategoryModel
     */
    id?: number;
    /**
     * The name of the category.
     * @type {string}
     * @memberof CategoryModel
     */
    name: string;
    /**
     * The last modified by.
     * @type {string}
     * @memberof CategoryModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof CategoryModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * 
     * @type {number}
     * @memberof CategoryModel
     */
    version?: number;
}

/**
 * Check if a given object implements the CategoryModel interface.
 */
export function instanceOfCategoryModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function CategoryModelFromJSON(json: any): CategoryModel {
    return CategoryModelFromJSONTyped(json, false);
}

export function CategoryModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): CategoryModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'createdBy': !exists(json, 'createdBy') ? undefined : json['createdBy'],
        'createdDate': !exists(json, 'createdDate') ? undefined : (new Date(json['createdDate'])),
        'id': !exists(json, 'id') ? undefined : json['id'],
        'name': json['name'],
        'lastModifiedBy': !exists(json, 'lastModifiedBy') ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': !exists(json, 'lastModifiedDate') ? undefined : (new Date(json['lastModifiedDate'])),
        'version': !exists(json, '__version') ? undefined : json['__version'],
    };
}

export function CategoryModelToJSON(value?: CategoryModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'name': value.name,
        '__version': value.version,
    };
}

