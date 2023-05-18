package com.bytechef.commons.typeconverter.conversion;

import com.bytechef.commons.typeconverter.TypeConverter;

/**
 * Convert to a {@link SqlTimestamp} by parsing a value as a string of
 * form <code>yyyy-[m]m-[d]d hh:mm:ss[.f...]</code>.
 *
 * @see	java.sql.Date#valueOf(String)
 *
 * @author Todd Fast
 */
public class SqlTimestampTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			java.sql.Timestamp.class,
			java.sql.Timestamp.class.getName(),
			TypeConverter.TYPE_SQL_TIMESTAMP
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof java.sql.Timestamp)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				// Value must be in the "yyyy-mm-dd hh:mm:ss.fffffffff"
				// format
				value=java.sql.Timestamp.valueOf(v);
			}
		}
		return value;
	}
}
