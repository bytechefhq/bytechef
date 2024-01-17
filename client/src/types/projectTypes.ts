import {
    ArrayPropertyModel,
    BooleanPropertyModel,
    type ControlTypeModel,
    DatePropertyModel,
    DateTimePropertyModel,
    DynamicPropertiesPropertyModel,
    FileEntryPropertyModel,
    IntegerPropertyModel,
    NullPropertyModel,
    NumberPropertyModel,
    ObjectPropertyModel,
    PropertyModel,
    StringPropertyModel,
    TaskPropertyModel,
    TimePropertyModel,
    ValuePropertyModel,
} from 'middleware/platform/configuration';

type PropertyTypeAll = ArrayPropertyModel &
    BooleanPropertyModel &
    DatePropertyModel &
    DateTimePropertyModel &
    DynamicPropertiesPropertyModel &
    FileEntryPropertyModel &
    IntegerPropertyModel &
    NumberPropertyModel &
    NullPropertyModel &
    ObjectPropertyModel &
    PropertyModel &
    StringPropertyModel &
    TaskPropertyModel &
    TimePropertyModel &
    ValuePropertyModel;

export type PropertyType = Omit<PropertyTypeAll, 'controlType'> & {
    controlType?: ControlTypeModel;
};
