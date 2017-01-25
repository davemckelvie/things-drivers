package nz.geek.android.things.drivers.i2c;

import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class Pcf8574Test {

  @Mock
  I2cDevice device;

  Pcf8574 pcf8574;

  @Before
  public void before() {
    pcf8574 = new Pcf8574(device);
  }

  @Test
  public void testSetPinDoesntMangleOtherPins() throws Exception {
    pcf8574.setPin(0, true);
    verify(device).write(new byte[]{0x01}, 1);
    assertEquals(0x01, pcf8574.readValue());
    pcf8574.setPin(1, true);
    verify(device).write(new byte[]{0x03}, 1);
    assertEquals(0x03, pcf8574.readValue());
    pcf8574.setPin(2, true);
    verify(device).write(new byte[]{0x07}, 1);
    assertEquals(0x07, pcf8574.readValue());
    pcf8574.setPin(3, true);
    verify(device).write(new byte[]{0x0F}, 1);
    assertEquals(0x0F, pcf8574.readValue());
    pcf8574.setPin(4, true);
    verify(device).write(new byte[]{0x1F}, 1);
    assertEquals(0x1F, pcf8574.readValue());
    pcf8574.setPin(5, true);
    verify(device).write(new byte[]{0x3F}, 1);
    assertEquals(0x3F, pcf8574.readValue());
    pcf8574.setPin(6, true);
    verify(device).write(new byte[]{0x7F}, 1);
    assertEquals(0x7F, pcf8574.readValue());
    pcf8574.setPin(7, true);
    verify(device).write(new byte[]{(byte) 0xFF}, 1);
    assertEquals(0xFF, pcf8574.readValue());
  }

  @Test
  public void testClearPinDoesntMangleOtherPins() throws Exception {
    pcf8574.writeByte(0x00, 0xFF);
    pcf8574.setPin(0, false);
    verify(device).write(new byte[]{(byte) 0xFE}, 1);
    pcf8574.setPin(1, false);
    verify(device).write(new byte[]{(byte) 0xFC}, 1);
    pcf8574.setPin(2, false);
    verify(device).write(new byte[]{(byte) 0xF8}, 1);
    pcf8574.setPin(3, false);
    verify(device).write(new byte[]{(byte) 0xF0}, 1);
    pcf8574.setPin(4, false);
    verify(device).write(new byte[]{(byte) 0xE0}, 1);
    pcf8574.setPin(5, false);
    verify(device).write(new byte[]{(byte) 0xC0}, 1);
    pcf8574.setPin(6, false);
    verify(device).write(new byte[]{(byte) 0x80}, 1);
    pcf8574.setPin(7, false);
    verify(device).write(new byte[]{0x00}, 1);
  }

  @Test
  public void testCloseClosesDevice() throws IOException {
    pcf8574.close();
    verify(device).close();
  }

  @Test
  public void testReadByte() throws IOException {
    pcf8574.readByte();
    verify(device).read(any(byte[].class), eq(1));
  }

  @Test
  public void testGetPin() throws IOException {
    pcf8574.getPin(1);
    verify(device).read(any(byte[].class), eq(1));
  }
}