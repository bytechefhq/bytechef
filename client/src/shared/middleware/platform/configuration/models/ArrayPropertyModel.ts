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
import type { ControlTypeModel } from './ControlTypeModel';
import {
    ControlTypeModelFromJSON,
    ControlTypeModelFromJSONTyped,
    ControlTypeModelToJSON,
} from './ControlTypeModel';
import type { OptionModel } from './OptionModel';
import {
    OptionModelFromJSON,
    OptionModelFromJSONTyped,
    OptionModelToJSON,
} from './OptionModel';
import type { OptionsDataSourceModel } from './OptionsDataSourceModel';
import {
    OptionsDataSourceModelFromJSON,
    OptionsDataSourceModelFromJSONTyped,
    OptionsDataSourceModelToJSON,
} from './OptionsDataSourceModel';
import type { PropertyModel } from './PropertyModel';
import {
    PropertyModelFromJSON,
    PropertyModelFromJSONTyped,
    PropertyModelToJSON,
} from './PropertyModel';
import type { PropertyTypeModel } from './PropertyTypeModel';
import {
    PropertyTypeModelFromJSON,
    PropertyTypeModelFromJSONTyped,
    PropertyTypeModelToJSON,
} from './PropertyTypeModel';
import type { ValuePropertyModel } from './ValuePropertyModel';
import {
    ValuePropertyModelFromJSON,
    ValuePropertyModelFromJSONTyped,
    ValuePropertyModelToJSON,
} from './ValuePropertyModel';

/**
 * An array property type.
 * @export
 * @interface ArrayPropertyModel
 */
export interface ArrayPropertyModel extends ValuePropertyModel {
    /**
     * The property default value.
     * @type {Array<object>}
     * @memberof ArrayPropertyModel
     */
    defaultValue?: Array<object>;
    /**
     * The property sample value.
     * @type {Array<object>}
     * @memberof ArrayPropertyModel
     */
    exampleValue?: Array<object>;
    /**
     * Types of the array items.
     * @type {Array<PropertyModel>}
     * @memberof ArrayPropertyModel
     */
    items?: Array<PropertyModel>;
    /**
     * 
     * @type {number}
     * @memberof ArrayPropertyModel
     */
    maxItems?: number;
    /**
     * 
     * @type {number}
     * @memberof ArrayPropertyModel
     */
    minItems?: number;
    /**
     * If the array can contain multiple items.
     * @type {boolean}
     * @memberof ArrayPropertyModel
     */
    multipleValues?: boolean;
    /**
     * The list of valid property options.
     * @type {Array<OptionModel>}
     * @memberof ArrayPropertyModel
     */
    options?: Array<OptionModel>;
    /**
     * 
     * @type {OptionsDataSourceModel}
     * @memberof ArrayPropertyModel
     */
    optionsDataSource?: OptionsDataSourceModel;
}

/**
 * Check if a given object implements the ArrayPropertyModel interface.
 */
export function instanceOfArrayPropertyModel(value: object): boolean {
    return true;
}

export function ArrayPropertyModelFromJSON(json: any): ArrayPropertyModel {
    return ArrayPropertyModelFromJSONTyped(json, false);
}

export function ArrayPropertyModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ArrayPropertyModel {
    if (json == null) {
        return json;
    }
    return {
        ...ValuePropertyModelFromJSONTyped(json, ignoreDiscriminator),
        'defaultValue': json['defaultValue'] == null ? undefined : json['defaultValue'],
        'exampleValue': json['exampleValue'] == null ? undefined : json['exampleValue'],
        'items': json['items'] == null ? undefined : ((json['items'] as Array<any>).map(PropertyModelFromJSON)),
        'maxItems': json['maxItems'] == null ? undefined : json['maxItems'],
        'minItems': json['minItems'] == null ? undefined : json['minItems'],
        'multipleValues': json['multipleValues'] == null ? undefined : json['multipleValues'],
        'options': json['options'] == null ? undefined : ((json['options'] as Array<any>).map(OptionModelFromJSON)),
        'optionsDataSource': json['optionsDataSource'] == null ? undefined : OptionsDataSourceModelFromJSON(json['optionsDataSource']),
    };
}

export function ArrayPropertyModelToJSON(value?: ArrayPropertyModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        ...ValuePropertyModelToJSON(value),
        'defaultValue': value['defaultValue'],
        'exampleValue': value['exampleValue'],
        'items': value['items'] == null ? undefined : ((value['items'] as Array<any>).map(PropertyModelToJSON)),
        'maxItems': value['maxItems'],
        'minItems': value['minItems'],
        'multipleValues': value['multipleValues'],
        'options': value['options'] == null ? undefined : ((value['options'] as Array<any>).map(OptionModelToJSON)),
        'optionsDataSource': OptionsDataSourceModelToJSON(value['optionsDataSource']),
    };
}

