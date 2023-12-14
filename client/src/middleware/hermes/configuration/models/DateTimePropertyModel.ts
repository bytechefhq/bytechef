/* tslint:disable */
/* eslint-disable */
/**
 * The Definition API
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
 * A date-time property type.
 * @export
 * @interface DateTimePropertyModel
 */
export interface DateTimePropertyModel extends ValuePropertyModel {
    /**
     * The property default value.
     * @type {Date}
     * @memberof DateTimePropertyModel
     */
    defaultValue?: Date;
    /**
     * The property sample value.
     * @type {Date}
     * @memberof DateTimePropertyModel
     */
    exampleValue?: Date;
    /**
     * The list of valid property options.
     * @type {Array<OptionModel>}
     * @memberof DateTimePropertyModel
     */
    options?: Array<OptionModel>;
    /**
     * 
     * @type {OptionsDataSourceModel}
     * @memberof DateTimePropertyModel
     */
    optionsDataSource?: OptionsDataSourceModel;
}

/**
 * Check if a given object implements the DateTimePropertyModel interface.
 */
export function instanceOfDateTimePropertyModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function DateTimePropertyModelFromJSON(json: any): DateTimePropertyModel {
    return DateTimePropertyModelFromJSONTyped(json, false);
}

export function DateTimePropertyModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): DateTimePropertyModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        ...ValuePropertyModelFromJSONTyped(json, ignoreDiscriminator),
        'defaultValue': !exists(json, 'defaultValue') ? undefined : (new Date(json['defaultValue'])),
        'exampleValue': !exists(json, 'exampleValue') ? undefined : (new Date(json['exampleValue'])),
        'options': !exists(json, 'options') ? undefined : ((json['options'] as Array<any>).map(OptionModelFromJSON)),
        'optionsDataSource': !exists(json, 'optionsDataSource') ? undefined : OptionsDataSourceModelFromJSON(json['optionsDataSource']),
    };
}

export function DateTimePropertyModelToJSON(value?: DateTimePropertyModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        ...ValuePropertyModelToJSON(value),
        'defaultValue': value.defaultValue === undefined ? undefined : (value.defaultValue.toISOString()),
        'exampleValue': value.exampleValue === undefined ? undefined : (value.exampleValue.toISOString()),
        'options': value.options === undefined ? undefined : ((value.options as Array<any>).map(OptionModelToJSON)),
        'optionsDataSource': OptionsDataSourceModelToJSON(value.optionsDataSource),
    };
}

