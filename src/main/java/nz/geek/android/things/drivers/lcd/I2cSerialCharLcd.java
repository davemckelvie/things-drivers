/*
 * Copyright 2016 Dave McKelvie <www.android.geek.nz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.geek.android.things.drivers.lcd;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.util.Log;

import nz.geek.android.things.drivers.i2c.Pcf8574;

public class I2cSerialCharLcd implements Lcd {

  private static final String TAG = "TingTing";
  private static final int ADDRESS = 0x07;      // PCF8574 A0-A2 for your board

  private static final int SPACE = 0x20;

  /**
   * Data and control line mapping and masks.
   *
   * Data does not affect port state when controlMask bit is 1
   *
   *   PCF8574 LCD         D_M   C_M1   C_M2
   *   ----------------------------------------
   *   P7      (14)D7      0      1      1
   *   P6      (13)D6      0      1      1
   *   P5      (12)D5      0      1      1
   *   P4      (11)D4      0      1      1
   *   P3      (-) BL      1      1      0
   *   P2      (6) E       1      0      1
   *   P1      (5) R/W     1      0      0
   *   P0      (4) RS      1      0      0
   *
   *   DATA_MASK           0x0F
   *   CONTROL_MASK_1      0xF8
   *   CONTROL_MASK_2      0xF4 (for display with E2)
   */
  private static final int DATA_MASK = 0x0F;
  private static final int CONTROL_MASK_1 = 0xF8;
  private static final int CONTROL_MASK_2 = 0xF4;
  private static final int BL_MASK = 0xF7;

  /**
   * PCF8574 to LCD pin mapping
   */
  private static final int LCD_BF = 0x80; // D7
  private static final int LCD_BACKLIGHT = 0x08;
  private static final int LCD_E  = 0x04;
  private static final int LCD_RW = 0x02;
  private static final int LCD_RS = 0x01;

  private static final int LCD_BF_PIN = 7;
  private static final int LCD_BACKLIGHT_PIN = 3;
  private static final int LCD_E_PIN  = 2;
  private static final int LCD_RW_PIN = 1;
  private static final int LCD_RS_PIN = 0;

  /**
   * command to select first or second half of 40x4 display
   */
  private static final int  LCD_DISPLAY_ONE = CONTROL_MASK_1;
  private static final int  LCD_DISPLAY_TWO = CONTROL_MASK_2;

  /**
   * command to set cursor to start of first line
   */
  @IntDef({LCD_LINE_ONE, LCD_LINE_TWO, LCD_LINE_THREE, LCD_LINE_FOUR})
  public @interface LcdLine {}
  public static final int  LCD_LINE_ONE = 0x80;
  public static final int  LCD_LINE_TWO = 0xC0;
  public static final int  LCD_LINE_THREE = 0x94;
  public static final int  LCD_LINE_FOUR = 0xD4;

  private static final int  LCD_CHAR_WIDTH      =     5;
  private static final int  LCD_CHAR_HEIGHT     =     7;

  /**
   * Commands used with {@writeCommend}
   */
  private static final int LCD_CLEAR_DISPLAY = 0x01;
  private static final int LCD_RETURN_HOME = 0x02;
  private static final int LCD_DECREMENT_DD_RAM = 0x04;
  private static final int LCD_INCREMENT_DD_RAM = 0x06;
  private static final int LCD_NO_SHIFT = 0x04;
  private static final int LCD_SHIFT = 0x05;
  private static final int LCD_DISPLAY_ON = 0x0C;
  private static final int LCD_DISPLAY_OFF = 0x08;
  private static final int LCD_CURSOR_ON = 0x0A;
  private static final int LCD_CURSOR_OFF = 0x08;
  private static final int LCD_BLINK_ON = 0x09;
  private static final int LCD_BLINK_OFF = 0x08;
  private static final int LCD_SHIFT_CURSOR_LEFT = 0x10;
  private static final int LCD_SHIFT_CURSOR_RIGHT = 0x14;
  private static final int LCD_SHIFT_DISPLAY_LEFT = 0x18;
  private static final int LCD_SHIFT_DISPLAY_RIGHT = 0x1C;
  private static final int LCD_8_BIT = 0x30;
  private static final int LCD_4_BIT = 0x20;
  private static final int LCD_2_LINE = 0x28;
  private static final int LCD_1_LINE = 0x20;
  private static final int LCD_5_10_DOTS = 0x24;
  private static final int LCD_5_7_DOTS = 0x20;
  private static final int LCD_SET_CG_RAM = 0x40;
  private static final int LCD_CG_RAM_ZERO = 0x00;
  private static final int LCD_CG_RAM_ONE = 0x08;
  private static final int LCD_CG_RAM_TWO = 0x10;
  private static final int LCD_CG_RAM_THREE = 0x18;
  private static final int LCD_CG_RAM_FOUR = 0x20;
  private static final int LCD_CG_RAM_FIVE = 0x28;
  private static final int LCD_CG_RAM_SIX = 0x30;
  private static final int LCD_CG_RAM_SEVEN = 0x38;
  private static final int LCD_SET_DD_RAM = 0x80;

  private Pcf8574 pcf8574;

  private final HandlerThread handlerThread = new HandlerThread(TAG);
  private Handler handler;
  private InitRunnable initRunnable = new InitRunnable();
  private int rs = 0;

  /**
   * 40x4 displays have two enable pins, they're effectively two displays
   */
  private int en = LCD_E;

  /**
   * control mask will change when using 40x4 displays with two enable pins
   */
  private int controlMask = CONTROL_MASK_1;

  private boolean initialised = false;
  private boolean doubleWrite = false;

  private final int width;
  private final int height;

  private I2cSerialCharLcd(int width, int height) {
    this.width = width;
    this.height = height;
    createPort();
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  /**
   * create the I2C IO port (PCF8574)
   */
  private void createPort() {
    pcf8574 = Pcf8574.create(ADDRESS);
  }

  public static I2cSerialCharLcd create(int width, int height) {
    return new I2cSerialCharLcd(width, height);
  }

  @Override
  public void connect() {
    if (pcf8574 == null) createPort();

    handler.post(initRunnable);
  }

  @Override
  public void disconnect() {
    handler.removeCallbacks(initRunnable);
    handlerThread.quitSafely();
    if (pcf8574 != null) {
      pcf8574.close();
      pcf8574 = null;
    }
  }

  @Override
  public void print(int position, String message) {
    writeCommand(position);
    for (int i = 0; i < message.length(); i++) {
      char c = message.charAt(i);
      write(c);
    }
  }

  /**
   * write byte to the display
   * @param data
   */
  private void write(int data) {
    pcf8574.writeByte(controlMask, rs);         // RS = rs, E = 0, R/W = 0
    pcf8574.writeByte(controlMask, rs | en);    // RS = rs, E = 1, R/W = 0
    pcf8574.writeByte(DATA_MASK, data);         // D0-7 = data
    pcf8574.writeByte(controlMask, rs);         // RS = rs, E = 0, R/W = 0

    if (doubleWrite) {
      pcf8574.writeByte(controlMask, rs | en);  // RS = rs, E = 1, R/W = 0
      pcf8574.writeByte(DATA_MASK, data << 4);
      pcf8574.writeByte(controlMask, rs);       // RS = rs, E = 0, R/W = 0
    }
  }

  private void writeCommand(int command) {
    rs = 0;
    write(command);
    rs = LCD_RS;
  }

  private int readAddressCounterAndBusyFlag() {
    // set D7 - D4 bits high before use as inputs in accordance with PCF8574 data sheet.
    pcf8574.writeByte(DATA_MASK, 0xFF);               // D0-7 = 0xFF
    pcf8574.writeByte(controlMask, LCD_RW);           // RS = 0, E = 0, R/W = 1
    pcf8574.writeByte(controlMask, LCD_RW | en);      // RS = 0, E = 1, R/W = 1
    int reg = pcf8574.readByte();                     // D7 = busy flag
    pcf8574.writeByte(controlMask, LCD_RW);           // RS = 0, E = 0, R/W = 1

    if (doubleWrite) {
      pcf8574.writeByte(controlMask, LCD_RW | en);    // RS = 0, E = 1, R/W = 1
      int dummy = pcf8574.readByte();
      pcf8574.writeByte(controlMask, LCD_RW);         // RS = 0, E = 0, R/W = 1
    }
    pcf8574.writeByte(0, 0);                          // D0-7 = 0, RS = 0, E = 0, R/W = 0

    Log.d(TAG, String.format("BF = %02X", reg & 0xFF));
    return reg;
  }

  private boolean readBusyFlag() {
    return (readAddressCounterAndBusyFlag() & LCD_BF) == LCD_BF;
  }

  private int readAddressCounter() {
    return (readAddressCounterAndBusyFlag() & 0x7F);
  }

  private final class InitRunnable implements Runnable {

    @Override
    public void run() {
      if (pcf8574 == null) return;

      if (initialised) return;

      doubleWrite = false;
      pcf8574.writeByte(0x00, 0x00);

      writeCommand(LCD_8_BIT);
      try {
        Thread.sleep(2);
      } catch (InterruptedException e) {
        // meh
      }
      writeCommand(LCD_8_BIT);
      writeCommand(LCD_8_BIT);

      writeCommand(LCD_4_BIT);
      doubleWrite = true;
      writeCommand(LCD_4_BIT | LCD_2_LINE);

      writeCommand(LCD_DISPLAY_OFF);
      writeCommand(LCD_CLEAR_DISPLAY);
      writeCommand(LCD_INCREMENT_DD_RAM);
      writeCommand(LCD_DISPLAY_ON | LCD_CURSOR_OFF | LCD_BLINK_OFF);
      initialised = true;
    }
  }

  @Override
  public void enableBackLight(boolean enable) {
    pcf8574.setPin(LCD_BACKLIGHT_PIN, enable);
  }

  /**
   * write a bit pattern to CGRAM
   *
   * bit pattern     eg          hex
   * 76543210
   * ---XXXXX        XXXX        1E
   * ---XXXXX        X   X       11
   * ---XXXXX        X   X       11
   * ---XXXXX        XXXX        1E
   * ---XXXXX        X   X       11
   * ---XXXXX        X   X       11
   * ---XXXXX        XXXX        1E
   *
   * @param address
   * @param pattern
   */
  @Override
  public void setCgRam(int address, byte[] pattern) {
    int ac = readAddressCounter();
    writeCommand(LCD_SET_CG_RAM | address);   // set CGRAM address
    for (int i = 0; i < 8; i++) {
      writeCommand(pattern[i]);
    }
    writeCommand(ac);                 // restore address counter
  }

  @Override
  public void clearLine(@LcdLine int line) {
    writeCommand(line);
    for (int i = 0; i < width; i++) {
      writeCommand(SPACE);
    }
  }
}
