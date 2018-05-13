package nz.geek.android.things.drivers.i2c;

import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static nz.geek.android.things.drivers.i2c.Tcs34725.AIEN;
import static nz.geek.android.things.drivers.i2c.Tcs34725.AILTL;
import static nz.geek.android.things.drivers.i2c.Tcs34725.ATIME;
import static nz.geek.android.things.drivers.i2c.Tcs34725.BLOCK_PROTOCOL;
import static nz.geek.android.things.drivers.i2c.Tcs34725.CONTROL;
import static nz.geek.android.things.drivers.i2c.Tcs34725.ENABLE;
import static nz.geek.android.things.drivers.i2c.Tcs34725.GAIN_1;
import static nz.geek.android.things.drivers.i2c.Tcs34725.GAIN_16;
import static nz.geek.android.things.drivers.i2c.Tcs34725.GAIN_4;
import static nz.geek.android.things.drivers.i2c.Tcs34725.GAIN_60;
import static nz.geek.android.things.drivers.i2c.Tcs34725.ID;
import static nz.geek.android.things.drivers.i2c.Tcs34725.PERS;
import static nz.geek.android.things.drivers.i2c.Tcs34725.STATUS;
import static nz.geek.android.things.drivers.i2c.Tcs34725.WEN;
import static nz.geek.android.things.drivers.i2c.Tcs34725.WTIME;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Tcs34725Test {

  @Mock
  I2cDevice device;

  Tcs34725 tcs34725;

  @Before
  public void before() throws IOException {
    when(device.readRegByte(ENABLE)).thenReturn((byte) 0x00);
    tcs34725 = new Tcs34725(device);
  }

  @Test
  public void setGain() throws Exception {
    tcs34725.setGain(-1);
    verify(device, never()).writeRegByte(CONTROL, (byte) GAIN_1);
    tcs34725.setGain(GAIN_1);
    verify(device).writeRegByte(CONTROL, (byte) GAIN_1);
    tcs34725.setGain(GAIN_4);
    verify(device).writeRegByte(CONTROL, (byte) GAIN_4);
    tcs34725.setGain(GAIN_16);
    verify(device).writeRegByte(CONTROL, (byte) GAIN_16);
    tcs34725.setGain(GAIN_60);
    verify(device).writeRegByte(CONTROL, (byte) GAIN_60);
    tcs34725.setGain(5);
    verify(device, never()).writeRegByte(CONTROL, (byte) 5);
  }

  @Test
  public void setIntegrationTime() throws Exception {
    tcs34725.setIntegrationTime(2.4f);
    verify(device).writeRegByte(ATIME, (byte) -2);
  }

  @Test
  public void setWaitTime() throws Exception {
    tcs34725.setWaitTime(4.8f);
    verify(device).writeRegByte(WTIME, (byte) -3);
  }

  @Test
  public void setInterruptThresholds() throws Exception {
    tcs34725.setInterruptThresholds(0x1122, 0x3344);
    verify(device).writeRegBuffer((AILTL | BLOCK_PROTOCOL), new byte[]{0x22, 0x11, 0x44, 0x33}, 4);
  }

  @Test
  public void setInterruptPersistence() throws Exception {
    for (int i = 0; i < 16; i++) {
      tcs34725.setInterruptPersistence(i);
      verify(device).writeRegByte(PERS, (byte) i);
    }
    tcs34725.setInterruptPersistence(-1);
    verify(device, never()).writeRegByte(PERS, (byte) -1);
    tcs34725.setInterruptPersistence(16);
    verify(device, never()).writeRegByte(PERS, (byte) 16);
  }

  @Test
  public void enableInterrupt() throws Exception {
    tcs34725.enableInterrupt(true);
    verify(device).readRegByte(ENABLE);
    verify(device).writeRegByte(ENABLE, (byte) AIEN);
  }

  @Test
  public void disableInterrupt() throws Exception {
    tcs34725.enableInterrupt(false);
    verify(device).readRegByte(ENABLE);
    verify(device).writeRegByte(ENABLE, (byte) 0x00);
  }

  @Test
  public void enableWaitTime() throws Exception {
    tcs34725.enableWaitTime(true);
    verify(device).readRegByte(ENABLE);
    verify(device).writeRegByte(ENABLE, (byte) WEN);
  }

  @Test
  public void disableWaitTime() throws Exception {
    tcs34725.enableWaitTime(false);
    verify(device).readRegByte(ENABLE);
    verify(device).writeRegByte(ENABLE, (byte) 0x00);
  }

  @Test
  public void readStatus() throws Exception {
    tcs34725.readStatus();
    verify(device).readRegByte(STATUS);
  }

  @Test
  public void readId() throws Exception {
    tcs34725.readId();
    verify(device).readRegByte(ID);
  }
}
