package com.toddfast.util.convert.conversion;

import com.toddfast.util.convert.TypeConverter;

/**
 * Convert to a {@link SqlTime} by parsing a value as a string of
 * form <code>hh:mm:ss</code>.
 *
 * @see	java.sql.Date#valueOf(String)
 *
 * @author Todd Fast
 */
public class SqlTimeTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			java.sql.Time.class,
			java.sql.Time.class.getName(),
			TypeConverter.TYPE_SQL_TIME
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof java.sql.Time)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				// Value must be in the "hh:mm:ss" format
				value=java.sql.Time.valueOf(v);
			}
		}
		return value;
	}
}
