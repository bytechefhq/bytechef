package com.bytechef.commons.typeconverter.conversion;

import com.bytechef.commons.typeconverter.TypeConverter;

/**
 * Convert to a {@link SqlDate} by parsing a value as a string of
 * form <code>yyyy-[m]m-[d]d</code>.
 *
 * @see	java.sql.Date#valueOf(String)
 *
 * @author Todd Fast
 */
public class SqlDateTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			java.sql.Date.class,
			java.sql.Date.class.getName(),
			TypeConverter.TYPE_SQL_DATE
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof java.sql.Date)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				// Value must be in the "yyyy-mm-dd" format
				value=java.sql.Date.valueOf(v);
			}
		}
		return value;
	}
}
