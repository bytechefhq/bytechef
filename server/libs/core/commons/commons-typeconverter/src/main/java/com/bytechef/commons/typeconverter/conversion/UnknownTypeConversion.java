package com.bytechef.commons.typeconverter.conversion;

import com.bytechef.commons.typeconverter.TypeConverter;

/**
 * Returns the value as-is (no conversion)
 *
 * @author Todd Fast
 */
public class UnknownTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] { TypeConverter.TYPE_UNKNOWN };
	}

	@Override
	public Object convert(Object value) {
		return value;
	}
}
