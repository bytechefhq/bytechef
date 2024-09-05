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
import type { OptionsDataSource } from './OptionsDataSource';
import {
    OptionsDataSourceFromJSON,
    OptionsDataSourceFromJSONTyped,
    OptionsDataSourceToJSON,
} from './OptionsDataSource';
import type { ControlType } from './ControlType';
import {
    ControlTypeFromJSON,
    ControlTypeFromJSONTyped,
    ControlTypeToJSON,
} from './ControlType';
import type { Option } from './Option';
import {
    OptionFromJSON,
    OptionFromJSONTyped,
    OptionToJSON,
} from './Option';
import type { PropertyType } from './PropertyType';
import {
    PropertyTypeFromJSON,
    PropertyTypeFromJSONTyped,
    PropertyTypeToJSON,
} from './PropertyType';
import type { ValueProperty } from './ValueProperty';
import {
    ValuePropertyFromJSON,
    ValuePropertyFromJSONTyped,
    ValuePropertyToJSON,
} from './ValueProperty';

/**
 * A time property.
 * @export
 * @interface TimeProperty
 */
export interface TimeProperty extends ValueProperty {
    /**
     * The property default value.
     * @type {string}
     * @memberof TimeProperty
     */
    defaultValue?: string;
    /**
     * The property sample value.
     * @type {string}
     * @memberof TimeProperty
     */
    exampleValue?: string;
    /**
     * The list of valid property options.
     * @type {Array<Option>}
     * @memberof TimeProperty
     */
    options?: Array<Option>;
    /**
     * 
     * @type {OptionsDataSource}
     * @memberof TimeProperty
     */
    optionsDataSource?: OptionsDataSource;
}



/**
 * Check if a given object implements the TimeProperty interface.
 */
export function instanceOfTimeProperty(value: object): value is TimeProperty {
    return true;
}

export function TimePropertyFromJSON(json: any): TimeProperty {
    return TimePropertyFromJSONTyped(json, false);
}

export function TimePropertyFromJSONTyped(json: any, ignoreDiscriminator: boolean): TimeProperty {
    if (json == null) {
        return json;
    }
    return {
        ...ValuePropertyFromJSONTyped(json, ignoreDiscriminator),
        'defaultValue': json['defaultValue'] == null ? undefined : json['defaultValue'],
        'exampleValue': json['exampleValue'] == null ? undefined : json['exampleValue'],
        'options': json['options'] == null ? undefined : ((json['options'] as Array<any>).map(OptionFromJSON)),
        'optionsDataSource': json['optionsDataSource'] == null ? undefined : OptionsDataSourceFromJSON(json['optionsDataSource']),
    };
}

export function TimePropertyToJSON(value?: TimeProperty | null): any {
    if (value == null) {
        return value;
    }
    return {
        ...ValuePropertyToJSON(value),
        'defaultValue': value['defaultValue'],
        'exampleValue': value['exampleValue'],
        'options': value['options'] == null ? undefined : ((value['options'] as Array<any>).map(OptionToJSON)),
        'optionsDataSource': OptionsDataSourceToJSON(value['optionsDataSource']),
    };
}

