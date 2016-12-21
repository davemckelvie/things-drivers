package nz.geek.android.things.drivers.i2c;

import com.google.android.things.pio.I2cDevice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class Pcf8574MaskTest {

  private int inputMask;
  private int inputData;
  private int expectedResult;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
            // mask, data, result, address, combinedAddress
            { 0x01, 0xFF, 0xFE },
            { 0x02, 0xFF, 0xFD },
            { 0x04, 0xFF, 0xFB },
            { 0x08, 0xFF, 0xF7 },
            { 0x10, 0xFF, 0xEF },
            { 0x20, 0xFF, 0xDF },
            { 0x40, 0xFF, 0xBF },
            { 0x80, 0xFF, 0x7F }
    });
  }

  public Pcf8574MaskTest(int inputMask, int inputData, int expectedResult) {
    this.inputMask = inputMask;
    this.inputData = inputData;
    this.expectedResult = expectedResult;
  }

  @Test
  public void testMask() throws Exception {
    I2cDevice mockDevice = mock(I2cDevice.class);
    Pcf8574 pcf8574 = new Pcf8574(mockDevice, 7);
    assertTrue(pcf8574.writeByte(inputMask, inputData));
    verify(mockDevice).write(new byte[]{(byte)(expectedResult & 0xFF)}, 1);
  }
}