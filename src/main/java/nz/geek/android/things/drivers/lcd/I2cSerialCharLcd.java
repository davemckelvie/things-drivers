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
import android.util.Log;

import nz.geek.android.things.drivers.i2c.Pcf8574;

public class I2cSerialCharLcd implements Lcd {

  private static final String TAG = "TingTing";

  private static final int SPACE = 0x20;

  /**
   * PCF8574 to LCD pin mapping
   */
  private static final int LCD_BF = 0x80; // D7

  /**
   * addresses for start of lines
   */
  public static final int  LCD_LINE_ONE = 0x00;
  public static final int  LCD_LINE_TWO = 0x40;

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

  /**
   * Read / Write pin bit value
   */
  private int rw = 0;

  /**
   * Register select pin bit value
   */
  private int rs = 0;

  /**
   * 40x4 displays have two enable pins, they're effectively two displays
   * this could be BV(ePin) or BV(e2Pin)
   */
  private int en = 0;

  /**
   * control mask will change when using 40x4 displays with two enable pins
   */
  private final int controlMask;

  /**
   * data mask bit value
   */
  private final int dataMask;

  private boolean initialised = false;
  private boolean doubleWrite = false;

  private final int width;
  private final int height;
  private final int address;
  private final boolean isPcf8574;
  private final boolean hasBackLight;

  /**
   * pin numbers [0:7]
   */
  private final int ePin;
  private final int e2Pin;
  private final int rsPin;
  private final int rwPin;
  private final int blPin;

  private I2cSerialCharLcd(int width, int height, int address,
                           int ePin, int e2Pin, int rsPin, int rwPin,
                           int d4Pin, int d5Pin, int d6Pin, int d7Pin,
                           boolean isPcf8574, boolean hasBl, int blPin) {
    this.width = width;
    this.height = height;
    this.address = address;
    this.isPcf8574 = isPcf8574;
    this.ePin = ePin;
    this.e2Pin = e2Pin;
    this.rsPin = rsPin;
    this.rwPin = rwPin;
    this.blPin = blPin;
    this.hasBackLight = hasBl;

    // setup the masks and bit positions from pin numbers
    en = BV(ePin);
    rs = BV(rsPin);
    rw = BV(rwPin);

    // initialise data and control masks, data does not affect port when mask bit is 1
    dataMask = ~(BV(d4Pin) | BV(d5Pin) | BV(d6Pin) | BV(d7Pin));
    controlMask = ~(en | rs | rw);

    createPort();
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());

    // TODO: if width x height is more than 80 it will have 2 Enables
  }

  /**
   * return the bit value of pin number
   * @param pin number [0:7]
   * @return bit value
   */
  private static int BV(int pin) {
    return (1 << pin);
  }

  /**
   * create the I2C IO port (PCF8574)
   */
  private void createPort() {
    pcf8574 = Pcf8574.create(address, isPcf8574);
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

  /**
   * print a message to the display
   * @param line the line number to print to
   * @param message the message to write
   */
  @Override
  public void print(int line, String message) {
    writeCommand(LCD_SET_DD_RAM | lineToAddress(line));
    for (int i = 0; i < message.length(); i++) {
      write(message.charAt(i));
    }
  }

  /**
   * write byte to the display
   * @param data
   */
  private void write(int data) {
    pcf8574.writeByte(controlMask, rs);         // RS = rs, E = 0, R/W = 0
    pcf8574.writeByte(controlMask, rs | en);    // RS = rs, E = 1, R/W = 0
    pcf8574.writeByte(dataMask, data);          // D0-7 = data
    pcf8574.writeByte(controlMask, rs);         // RS = rs, E = 0, R/W = 0

    if (doubleWrite) {
      pcf8574.writeByte(controlMask, rs | en);  // RS = rs, E = 1, R/W = 0
      pcf8574.writeByte(dataMask, data << 4);
      pcf8574.writeByte(controlMask, rs);       // RS = rs, E = 0, R/W = 0
    }
  }

  private void writeCommand(int command) {
    rs = 0;
    write(command);
    rs = BV(rsPin);
  }

  /**
   * convert display line number to DDRAM address
   * @param line line number [1:height]
   * @return DDRAM address for the start of line
   */
  private int lineToAddress(int line) {
    switch(line) {
      case 1:
        return LCD_LINE_ONE;
      case 2:
        return LCD_LINE_TWO;
      case 3:
        return LCD_LINE_ONE + width;
      case 4:
        return LCD_LINE_TWO + width;
      default:
        return LCD_LINE_ONE;
    }
  }

  private int readAddressCounterAndBusyFlag() {
    // set D7 - D4 bits high before use as inputs in accordance with PCF8574 data sheet.
    pcf8574.writeByte(dataMask, 0xFF);               // D0-7 = 0xFF
    pcf8574.writeByte(controlMask, rw);           // RS = 0, E = 0, R/W = 1
    pcf8574.writeByte(controlMask, rw | en);      // RS = 0, E = 1, R/W = 1
    int reg = pcf8574.readByte();                     // D7 = busy flag
    pcf8574.writeByte(controlMask, rw);           // RS = 0, E = 0, R/W = 1

    if (doubleWrite) {
      pcf8574.writeByte(controlMask, rw | en);    // RS = 0, E = 1, R/W = 1
      int dummy = pcf8574.readByte();
      pcf8574.writeByte(controlMask, rw);         // RS = 0, E = 0, R/W = 1
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
    if (hasBackLight) {
      pcf8574.setPin(blPin, enable);
    }
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
  public void clearLine(int line) {
    writeCommand(lineToAddress(line));
    for (int i = 0; i < width; i++) {
      writeCommand(SPACE);
    }
  }

  @Override
  public void clearDisplay() {
    writeCommand(LCD_CLEAR_DISPLAY);
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  public static I2cSerialCharLcdBuilder builder(int width, int height) {
    return new I2cSerialCharLcdBuilder(width, height);
  }

  public static final class I2cSerialCharLcdBuilder {
    private int width;
    private int height;

    // port pin numbers [0:7]
    private int e1Pin;
    private int e2Pin;
    private int rsPin;
    private int rwPin;
    private int blPin;
    private int d4Pin, d5Pin, d6Pin, d7Pin;

    private int address;
    private boolean isPcf8574 = false; // i.e., not pcf8574A, default to no
    private boolean hasBackLight = false;

    /*package*/ I2cSerialCharLcdBuilder(int width, int height) {
      this.width = width;
      this.height = height;
    }

    public I2cSerialCharLcdBuilder isPcf8574(boolean isPcf8574) {
      this.isPcf8574 = isPcf8574;
      return this;

    }
    public I2cSerialCharLcdBuilder e(int e1Pin) {
      this.e1Pin = e1Pin;
      return this;
    }

    public I2cSerialCharLcdBuilder e2(int e2Pin) {
      this.e2Pin = e2Pin;
      return this;
    }

    public I2cSerialCharLcdBuilder rs(int rsPin) {
      this.rsPin = rsPin;
      return this;
    }

    public I2cSerialCharLcdBuilder rw(int rwPin) {
      this.rwPin = rwPin;
      return this;
    }

    public I2cSerialCharLcdBuilder bl(int blPin) {
      this.blPin = blPin;
      hasBackLight = true;
      return this;
    }

    public I2cSerialCharLcdBuilder data(int d4Pin, int d5Pin, int d6Pin, int d7Pin) {
      this.d4Pin = d4Pin;
      this.d5Pin = d5Pin;
      this.d6Pin = d6Pin;
      this.d7Pin = d7Pin;
      return this;
    }

    public I2cSerialCharLcdBuilder address(int address) {
      this.address = address;
      return this;
    }

    public I2cSerialCharLcd build() {
      return new I2cSerialCharLcd(width, height, address, e1Pin, e2Pin, rsPin, rwPin,
              d4Pin, d5Pin, d6Pin, d7Pin, isPcf8574, hasBackLight, blPin);
    }

  }
}
