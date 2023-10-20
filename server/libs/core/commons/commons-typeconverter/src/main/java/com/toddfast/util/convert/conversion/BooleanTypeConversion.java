package com.toddfast.util.convert.conversion;

import com.toddfast.util.convert.TypeConverter;

/**
 * Convert to a boolean by parsing the value as a string
 *
 * @author Todd Fast
 */
public class BooleanTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			Boolean.class,
			Boolean.TYPE,
			Boolean.class.getName(),
			TypeConverter.TYPE_BOOLEAN
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof Boolean)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=Boolean.parseBoolean(v);
			}
		}
		return value;
	}
}
