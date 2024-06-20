package com.bytechef.component.data.mapper.util;

import com.bytechef.component.definition.Parameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE;
import static com.bytechef.component.definition.ComponentDSL.nullable;

public class DataMapperUtils {
    public static Class<?> getType(Parameters inputParameters) {
        return switch (inputParameters.getRequiredInteger(TYPE)) {
            case 1 -> ArrayList.class;
            case 2 -> Boolean.class;
            case 3 -> LocalDate.class;
            case 4 -> LocalDateTime.class;
            case 5 -> Integer.class;
            case 7 -> Number.class;
            case 8 -> Object.class;
            case 9 -> String.class;
            case 10 -> LocalTime.class;
            default -> nullable().getClass();
        };
    }

    public static Object getValue(Parameters inputParameters) {
        Object value = null;

        switch (inputParameters.getRequiredInteger(TYPE)) {
            case 1:
                value = inputParameters.getRequiredArray(VALUE);
                break;
            case 2:
                value = inputParameters.getRequiredBoolean(VALUE);
                break;
            case 3:
                value = inputParameters.getRequiredLocalDate(VALUE);
                break;
            case 4:
                value = inputParameters.getRequiredLocalDateTime(VALUE);
                break;
            case 5:
                value = inputParameters.getRequiredInteger(VALUE);
                break;
            case 6:
                value = nullable();
                break;
            case 7:
                value = inputParameters.getRequiredDouble(VALUE);
                break;
            case 8:
                value = inputParameters.getRequiredMap(VALUE);
                break;
            case 9:
                value = inputParameters.getRequiredString(VALUE);
                break;
            case 10:
                value = inputParameters.getRequiredLocalTime(VALUE);
                break;
            default:
                break;
        }

        return value;
    }
}
