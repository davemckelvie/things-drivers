package nz.geek.android.things.drivers.i2c;

import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class Pcf9685Test {

  @Mock
  I2cDevice device;

  Pcf9685 pcf9685;

  @Before
  public void setUp() throws Exception {
    pcf9685 = new Pcf9685(device);
    doReturn((byte)0x20).when(device).readRegByte(anyInt());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMinFrequency() throws IOException {
    pcf9685.setPwmFrequencyHz(23);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxFrequency() throws IOException {
    pcf9685.setPwmFrequencyHz(1527);
  }

  @Test
  public void testSetFrequency() throws IOException {
    pcf9685.setPwmFrequencyHz(200);
    verify(device).writeRegByte(0, (byte) 0x30);  // sleep
    verify(device).writeRegByte(0xFE, (byte) 30);  // prescale
    verify(device).writeRegByte(0, (byte) 0x20);  // auto increment
  }

  @Test
  public void testPinPwmOnOff() throws IOException {
    // from datasheet example 1
    pcf9685.setPinPwmOnOff(409, 1228, 0);
    verify(device).writeRegBuffer(6, new byte[]{(byte)0x99, 1, (byte)0xCC, 4}, 4);
  }

  @Test
  public void testLedRegisterMapping() throws IOException {
    byte[] buffer = new byte[] {(byte)0x99, 1, (byte)0xCC, 4};
    pcf9685.setPinPwmOnOff(409, 1228, 0);
    verify(device).writeRegBuffer(0x06, buffer, 4);
    pcf9685.setPinPwmOnOff(409, 1228, 1);
    verify(device).writeRegBuffer(0x0A, buffer, 4);
    pcf9685.setPinPwmOnOff(409, 1228, 2);
    verify(device).writeRegBuffer(0x0E, buffer, 4);
    pcf9685.setPinPwmOnOff(409, 1228, 3);
    verify(device).writeRegBuffer(0x12, buffer, 4);
  }
}