package com.bytechef.commons.typeconverter.conversion;

import com.bytechef.commons.typeconverter.TypeConverter;

/**
 * Returns the value as-is (no conversion)
 *
 * @author Todd Fast
 */
public class IdentityTypeConversion
		implements TypeConverter.Conversion<Object> {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {};
	}

	@Override
	public Object convert(Object value) {
		return value;
	}
}
