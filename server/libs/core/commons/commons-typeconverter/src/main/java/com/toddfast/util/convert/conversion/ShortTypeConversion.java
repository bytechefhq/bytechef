package com.toddfast.util.convert.conversion;

import com.toddfast.util.convert.TypeConverter;

/**
 * Convert to a short by parsing the value as a string
 *
 * @author Todd Fast
 */
public class ShortTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			Short.class,
			Short.TYPE,
			Short.class.getName(),
			TypeConverter.TYPE_SHORT
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof Short)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=Short.parseShort(v);
			}
		}
		return value;
	}
}
