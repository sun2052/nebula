package org.byteinfo.util.codec;

import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ByteUtilTest
 */
public class ByteUtilTest {
	@Test
	public void testByteUtil() {
		assertArrayEquals(new byte[] {(byte) 0x11, (byte) 0x22}, ByteUtil.asBytes((short) 0x11_22));
		assertArrayEquals(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44}, ByteUtil.asBytes(0x11_22_33_44));
		assertArrayEquals(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88}, ByteUtil.asBytes(0x11_22_33_44_55_66_77_88L));
		assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0x0D, (byte) 0x00, (byte) 0x0A}, ByteUtil.asUTF16Bytes("\r\n"));

		assertArrayEquals(new byte[] {(byte) 0x11, (byte) 0x22}, ByteUtil.asBytes((short) 0x11_22, ByteOrder.BIG_ENDIAN));
		assertArrayEquals(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44}, ByteUtil.asBytes(0x11_22_33_44, ByteOrder.BIG_ENDIAN));
		assertArrayEquals(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88}, ByteUtil.asBytes(0x11_22_33_44_55_66_77_88L, ByteOrder.BIG_ENDIAN));
		assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0x0D, (byte) 0x00, (byte) 0x0A}, ByteUtil.asUTF16Bytes("\r\n", ByteOrder.BIG_ENDIAN));

		assertArrayEquals(new byte[] {(byte) 0x22, (byte) 0x11}, ByteUtil.asBytes((short) 0x11_22, ByteOrder.LITTLE_ENDIAN));
		assertArrayEquals(new byte[] {(byte) 0x44, (byte) 0x33, (byte) 0x22, (byte) 0x11}, ByteUtil.asBytes(0x11_22_33_44, ByteOrder.LITTLE_ENDIAN));
		assertArrayEquals(new byte[] {(byte) 0x88, (byte) 0x77, (byte) 0x66, (byte) 0x55, (byte) 0x44, (byte) 0x33, (byte) 0x22, (byte) 0x11}, ByteUtil.asBytes(0x11_22_33_44_55_66_77_88L, ByteOrder.LITTLE_ENDIAN));
		assertArrayEquals(new byte[] {(byte) 0x0D, (byte) 0x00, (byte) 0x0A, (byte) 0x00}, ByteUtil.asUTF16Bytes("\r\n", ByteOrder.LITTLE_ENDIAN));

		assertEquals((short) 0x11_22, ByteUtil.asShort(new byte[] {(byte) 0x11, (byte) 0x22}));
		assertEquals(0x11_22_33_44, ByteUtil.asInt(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44}));
		assertEquals(0x11_22_33_44_55_66_77_88L, ByteUtil.asLong(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88}));
		assertEquals("\r\n", ByteUtil.asStringByUTF16Bytes(new byte[] {(byte) 0x00, (byte) 0x0D, (byte) 0x00, (byte) 0x0A}));

		assertEquals((short) 0x11_22, ByteUtil.asShort(new byte[] {(byte) 0x11, (byte) 0x22}, ByteOrder.BIG_ENDIAN));
		assertEquals(0x11_22_33_44, ByteUtil.asInt(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44}, ByteOrder.BIG_ENDIAN));
		assertEquals(0x11_22_33_44_55_66_77_88L, ByteUtil.asLong(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88}, ByteOrder.BIG_ENDIAN));
		assertEquals("\r\n", ByteUtil.asStringByUTF16Bytes(new byte[] {(byte) 0x00, (byte) 0x0D, (byte) 0x00, (byte) 0x0A}, ByteOrder.BIG_ENDIAN));

		assertEquals((short) 0x11_22, ByteUtil.asShort(new byte[] {(byte) 0x22, (byte) 0x11}, ByteOrder.LITTLE_ENDIAN));
		assertEquals(0x11_22_33_44, ByteUtil.asInt(new byte[] {(byte) 0x44, (byte) 0x33, (byte) 0x22, (byte) 0x11}, ByteOrder.LITTLE_ENDIAN));
		assertEquals(0x11_22_33_44_55_66_77_88L, ByteUtil.asLong(new byte[] {(byte) 0x88, (byte) 0x77, (byte) 0x66, (byte) 0x55, (byte) 0x44, (byte) 0x33, (byte) 0x22, (byte) 0x11}, ByteOrder.LITTLE_ENDIAN));
		assertEquals("\r\n", ByteUtil.asStringByUTF16Bytes(new byte[] {(byte) 0x0D, (byte) 0x00, (byte) 0x0A, (byte) 0x00}, ByteOrder.LITTLE_ENDIAN));

		assertEquals((short) 0x11_22, ByteUtil.asUnsignedShort(new byte[] {(byte) 0x11, (byte) 0x22}));
		assertEquals(0x11_22_33_44, ByteUtil.asUnsignedInt(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44}));

		assertEquals((short) 0x11_22, ByteUtil.asUnsignedShort(new byte[] {(byte) 0x11, (byte) 0x22}, ByteOrder.BIG_ENDIAN));
		assertEquals(0x11_22_33_44, ByteUtil.asUnsignedInt(new byte[] {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44}, ByteOrder.BIG_ENDIAN));

		assertEquals((short) 0x11_22, ByteUtil.asUnsignedShort(new byte[] {(byte) 0x22, (byte) 0x11}, ByteOrder.LITTLE_ENDIAN));
		assertEquals(0x11_22_33_44, ByteUtil.asUnsignedInt(new byte[] {(byte) 0x44, (byte) 0x33, (byte) 0x22, (byte) 0x11}, ByteOrder.LITTLE_ENDIAN));

		assertArrayEquals(new byte[] {(byte) 0xF0, (byte) 0x90, (byte) 0x8D, (byte) 0x88}, ByteUtil.asUTF8BytesByCodePoint(0x10348));
		assertEquals(0x10348, ByteUtil.asCodePointByUTF8Bytes(new byte[] {(byte) 0xF0, (byte) 0x90, (byte) 0x8D, (byte) 0x88}));
	}
}
