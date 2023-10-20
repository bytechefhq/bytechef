package com.toddfast.util.convert;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Todd Fast
 */
public class TypeConverterTest {

	@Test
	public void testGeneral() {
		Object in;
		Object out;

		in=new Integer(12);
		out=TypeConverter.asString(in);
		assertTrue(out instanceof String);
		assertEquals("12",out);

		in="12";
		out=TypeConverter.convert(Integer.class,in);
		assertTrue(out instanceof Integer);
		assertEquals(12,out);

		in="12";
		out=TypeConverter.convert(Long.class,in);
		assertTrue(out instanceof Long);
		assertEquals(12L,out);

		in="12";
		out=TypeConverter.convert(Short.class,in);
		assertTrue(out instanceof Short);
		assertEquals((short)12,out);

		in="12";
		out=TypeConverter.convert(Byte.class,in);
		assertTrue(out instanceof Byte);
		assertEquals((byte)12,out);

		in="12.0";
		out=TypeConverter.convert(Float.class,in);
		assertTrue(out instanceof Float);
		assertEquals(12.0f,out);

		in="12.0";
		out=TypeConverter.convert(Double.class,in);
		assertTrue(out instanceof Double);
		assertEquals(12.0d,out);

		String big="1234567890123456789012345678901234567890123456789012345678";
		in=big;
		out=TypeConverter.convert(BigDecimal.class,in);
		assertTrue(out instanceof BigDecimal);
		assertEquals(new BigDecimal(big),out);

		in="true";
		out=TypeConverter.convert(Boolean.class,in);
		assertTrue(out instanceof Boolean);
		assertEquals(true,out);

		in="c";
		out=TypeConverter.convert(Character.class,in);
		assertTrue(out instanceof Character);
		assertEquals('c',out);

		in=new TestSubclass();
		out=TypeConverter.convert(String.class,in);
		assertTrue(out instanceof String);
		assertEquals("Converted Test value",out);

		// There should be no conversion here
		in=new TestSubclass();
		out=TypeConverter.convert(TestSuperclass.class,in);
		assertTrue(out instanceof TestSuperclass);
		assertEquals(in,out);
	}

	@Test
	public void testConversion() {
		StringWrapper.TypeConversion conversion=
			new StringWrapper.TypeConversion();
		TypeConverter.registerTypeConversion(conversion);

		final String IN="Hello, world";
		final StringWrapper out=TypeConverter.convert(StringWrapper.class,IN);
		assertEquals(StringWrapper.getWrappedString(IN),out.toString());
	}

	@Test
	public void testConversionUnregistration() throws IllegalArgumentException {
		NaughtyConversion conversion=new NaughtyConversion();
		TypeConverter.registerTypeConversion(conversion);
		TypeConverter.registerTypeConversion("foobar",conversion);

		conversion.setTypeKeys(new Object[0]);

		// This should unregister it even though the keys are bogus
		TypeConverter.unregisterTypeConversion(conversion);

		// Throw exception
		TypeConverter.convert(Bogus.class,"don't care");
	}

	@Test
	public void testConverter() {

		Object in;
		Object out;

		TypeConverter.Conversion<String> converter=
			TypeConverter.to(String.class);

		in=12;
		out=converter.convert(in);
		assertEquals("12",out);

		in=13;
		out=converter.convert(in);
		assertEquals("13",out);

		in="14";
		out=converter.convert(in);
		assertEquals("14",out);

		in=1.1f;
		out=converter.convert(in);
		assertEquals("1.1",out);
	}

	@Test
	public void testBogusInteger() throws IllegalArgumentException {
		TypeConverter.asInt("bogus");
	}

	@Test
	public void testBogusIntegerWithDefaultValue() {
		int out=TypeConverter.asInt(null,12);
		assertEquals(12,out);
	}
}
