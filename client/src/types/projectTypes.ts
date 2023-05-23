import {
    AnyPropertyModel,
    ArrayPropertyModel,
    BooleanPropertyModel,
    DatePropertyModel,
    DateTimePropertyModel,
    DynamicPropertiesPropertyModel,
    IntegerPropertyModel,
    NullPropertyModel,
    NumberPropertyModel,
    ObjectPropertyModel,
    StringPropertyModel,
    TimePropertyModel,
} from 'middleware/core/definition-registry';

export type PropertyType = AnyPropertyModel &
    ArrayPropertyModel &
    BooleanPropertyModel &
    DatePropertyModel &
    DateTimePropertyModel &
    DynamicPropertiesPropertyModel &
    IntegerPropertyModel &
    NumberPropertyModel &
    NullPropertyModel &
    ObjectPropertyModel &
    StringPropertyModel &
    TimePropertyModel;
