package com.toddfast.util.convert.conversion;

import com.toddfast.util.convert.TypeConverter;

/**
 * Convert to a long by parsing the value as a string
 *
 * @author Todd Fast
 */
public class LongTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			Long.class,
			Long.TYPE,
			Long.class.getName(),
			TypeConverter.TYPE_LONG
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof Long)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=Long.parseLong(v);
			}
		}
		return value;
	}
}
