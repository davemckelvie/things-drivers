package nz.geek.android.things.drivers.i2c;

import com.google.android.things.pio.I2cDevice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class Pcf8574SetPinTest {

  private int pin;
  private int expectedData;
  private boolean state;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
            // pin, state, expected data
            { 0, true, 0x01 },
            { 0, false, 0x00 },
            { 1, true, 0x02 },
            { 1, false, 0x00 },
            { 2, true, 0x04 },
            { 2, false, 0x00 },
            { 3, true, 0x08 },
            { 3, false, 0x00 },
            { 4, true, 0x10 },
            { 4, false, 0x00 },
            { 5, true, 0x20 },
            { 5, false, 0x00 },
            { 6, true, 0x40 },
            { 6, false, 0x00 },
            { 7, true, 0x80 },
            { 7, false, 0x00 },

    });
  }

  public Pcf8574SetPinTest(int pin, boolean state, int expectedData) {
    this.pin = pin;
    this.state = state;
    this.expectedData = expectedData;
  }

  @Test
  public void testSetPin() throws Exception{
    I2cDevice mockDevice = mock(I2cDevice.class);
    Pcf8574 pcf8574 = new Pcf8574(mockDevice, 7);
    pcf8574.setPin(pin, state);
    verify(mockDevice).write(new byte[]{(byte) expectedData}, 1);
    assertEquals(expectedData, pcf8574.readValue());
  }
}