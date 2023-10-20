import {
    ArrayPropertyModel,
    BooleanPropertyModel,
    DatePropertyModel,
    DateTimePropertyModel,
    DynamicPropertiesPropertyModel,
    IntegerPropertyModel,
    NullPropertyModel,
    NumberPropertyModel,
    ObjectPropertyModel,
    OneOfPropertyModel,
    StringPropertyModel,
    TimePropertyModel,
} from 'middleware/definition-registry';

export type PropertyType = ArrayPropertyModel &
    BooleanPropertyModel &
    DatePropertyModel &
    DateTimePropertyModel &
    DynamicPropertiesPropertyModel &
    IntegerPropertyModel &
    NumberPropertyModel &
    NullPropertyModel &
    ObjectPropertyModel &
    OneOfPropertyModel &
    StringPropertyModel &
    TimePropertyModel;
