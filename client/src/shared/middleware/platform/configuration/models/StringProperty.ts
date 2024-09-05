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
 * A string property.
 * @export
 * @interface StringProperty
 */
export interface StringProperty extends ValueProperty {
    /**
     * The language id used together with CODE_EDITOR control type.
     * @type {string}
     * @memberof StringProperty
     */
    languageId?: string;
    /**
     * The property default value.
     * @type {string}
     * @memberof StringProperty
     */
    defaultValue?: string;
    /**
     * The property sample value.
     * @type {string}
     * @memberof StringProperty
     */
    exampleValue?: string;
    /**
     * The maximum string length.
     * @type {number}
     * @memberof StringProperty
     */
    maxLength?: number;
    /**
     * The minimum string length.
     * @type {number}
     * @memberof StringProperty
     */
    minLength?: number;
    /**
     * The list of valid property options.
     * @type {Array<Option>}
     * @memberof StringProperty
     */
    options?: Array<Option>;
    /**
     * 
     * @type {OptionsDataSource}
     * @memberof StringProperty
     */
    optionsDataSource?: OptionsDataSource;
}



/**
 * Check if a given object implements the StringProperty interface.
 */
export function instanceOfStringProperty(value: object): value is StringProperty {
    return true;
}

export function StringPropertyFromJSON(json: any): StringProperty {
    return StringPropertyFromJSONTyped(json, false);
}

export function StringPropertyFromJSONTyped(json: any, ignoreDiscriminator: boolean): StringProperty {
    if (json == null) {
        return json;
    }
    return {
        ...ValuePropertyFromJSONTyped(json, ignoreDiscriminator),
        'languageId': json['languageId'] == null ? undefined : json['languageId'],
        'defaultValue': json['defaultValue'] == null ? undefined : json['defaultValue'],
        'exampleValue': json['exampleValue'] == null ? undefined : json['exampleValue'],
        'maxLength': json['maxLength'] == null ? undefined : json['maxLength'],
        'minLength': json['minLength'] == null ? undefined : json['minLength'],
        'options': json['options'] == null ? undefined : ((json['options'] as Array<any>).map(OptionFromJSON)),
        'optionsDataSource': json['optionsDataSource'] == null ? undefined : OptionsDataSourceFromJSON(json['optionsDataSource']),
    };
}

export function StringPropertyToJSON(value?: StringProperty | null): any {
    if (value == null) {
        return value;
    }
    return {
        ...ValuePropertyToJSON(value),
        'languageId': value['languageId'],
        'defaultValue': value['defaultValue'],
        'exampleValue': value['exampleValue'],
        'maxLength': value['maxLength'],
        'minLength': value['minLength'],
        'options': value['options'] == null ? undefined : ((value['options'] as Array<any>).map(OptionToJSON)),
        'optionsDataSource': OptionsDataSourceToJSON(value['optionsDataSource']),
    };
}

