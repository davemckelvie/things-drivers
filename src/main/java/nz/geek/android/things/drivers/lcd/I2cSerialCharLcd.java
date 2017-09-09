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

import com.google.android.things.pio.PeripheralManagerService;

import nz.geek.android.things.drivers.display.CharacterDisplay;
import nz.geek.android.things.drivers.i2c.Pcf8574;

import static nz.geek.android.things.drivers.i2c.Pcf8574.BV;

public class I2cSerialCharLcd implements CharacterDisplay {
  
  private static final int SPACE = 0x20;

  /**
   * PCF8574 to LCD pin mapping
   */
  private static final int LCD_BF = 0x80; // D7

  /**
   * DDRAM addresses for start of lines 1 and 2
   */
  public static final int  LCD_LINE_ONE = 0x00;
  public static final int  LCD_LINE_TWO = 0x40;

  /**
   * Commands used with {@link #writeCommand(int)}
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
  private static final int LCD_SET_DD_RAM = 0x80;

  private Pcf8574 pcf8574;

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
  private int controlMask;

  /**
   * data mask bit value
   */
  private final int dataMask;

  private boolean doubleWrite = false;

  private final int width;
  private final int height;
  private final int address;
  private final boolean isPcf8574;
  private final boolean hasBackLight;
  private final String bus;

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
                           boolean isPcf8574, boolean hasBl, int blPin,
                           String bus) {
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
    this.bus = bus;

    // setup the masks and bit positions from pin numbers
    en = BV(ePin);
    rs = BV(rsPin);
    rw = BV(rwPin);

    // initialise data and control masks, data does not affect port when mask bit is 1
    dataMask = ~(BV(d4Pin) | BV(d5Pin) | BV(d6Pin) | BV(d7Pin));
    controlMask = ~(en | rs | rw);

    createPort();
  }

  /**
   * create the I2C IO port (PCF8574)
   */
  private void createPort() {
    if (bus != null) {
      pcf8574 = Pcf8574.create(address, bus, isPcf8574);
    } else {
      pcf8574 = Pcf8574.create(address, isPcf8574);
    }
  }

  private boolean isDoubleDisplay() {
    return (width * height > 80);
  }

  @Override
  public void connect() {
    if (pcf8574 == null) createPort();
    init();
  }

  @Override
  public void disconnect() {
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
  public synchronized void print(int line, String message) {

    // support displays with more than 80 characters (will have 2 enable pins)
    if (isDoubleDisplay() && line > 2) {
      // lines 3 and 4 only
      line -= 2;
      switchDisplay(2);
    }

    writeCommand(LCD_SET_DD_RAM | lineToAddress(line));
    for (int i = 0; i < message.length(); i++) {
      write(message.charAt(i));
    }

    // always default to first display, won't have an effect on displays with less than
    // 80 characters
    switchDisplay(1);
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

  private void switchDisplay(int display) {
    if (display == 2) {
      en = BV(e2Pin);
    } else {
      en = BV(ePin);
    }
    controlMask = ~(en | rs | rw);
  }

  private void init() {
    if (pcf8574 == null) return;

    switchDisplay(1);
    pcf8574.writeByte(0x00, 0x00);
    initialiseLcd();
    if (isDoubleDisplay()) {
      switchDisplay(2);
      initialiseLcd();
      switchDisplay(1);
    }
  }

  private void initialiseLcd() {

    doubleWrite = false;

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
  }

  @Override
  public void enableBackLight(boolean enable) {
    if (hasBackLight) {
      pcf8574.setPin(blPin, enable);
    }
  }

  @Override
  public boolean hasBackLight() {
    return hasBackLight;
  }

  private void setCgRamPattern(int address, byte[] pattern) {
    writeCommand(LCD_SET_CG_RAM | address);   // set CGRAM address
    for (byte aPattern : pattern) {
      write(aPattern);
    }
  }

  @Override
  public void setCustomCharacter(int address, byte[] pattern) {
    setCgRamPattern(address, pattern);
    if (isDoubleDisplay()) {
      switchDisplay(2);
      setCgRamPattern(address, pattern);
      switchDisplay(1);
    }
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

  @Override
  public void initBarGraph() {
    setCustomCharacter(0x00, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
    setCustomCharacter(0x08, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x00});
    setCustomCharacter(0x10, new byte[]{0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x00});
    setCustomCharacter(0x18, new byte[]{0x1C, 0x1C, 0x1C, 0x1C, 0x1C, 0x1C, 0x1C, 0x00});
    setCustomCharacter(0x20, new byte[]{0x1E, 0x1E, 0x1E, 0x1E, 0x1E, 0x1E, 0x1E, 0x00});
    setCustomCharacter(0x28, new byte[]{0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x00});
  }

  public static I2cSerialCharLcdBuilder builder(int width, int height) {
    return new I2cSerialCharLcdBuilder(width, height);
  }

  /**
   * Builder used to create a {@link #I2cSerialCharLcd(int, int, int, int, int, int, int, int, int, int, int, boolean, boolean, int, String)}
   */
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
    private String bus = null;

    /*package*/ I2cSerialCharLcdBuilder(int width, int height) {
      this.width = width;
      this.height = height;
    }

    /**
     * Specify that the LCD is connected with a PCF8574. This uses a different
     * base address to the more typical PCF8574A.
     * @param isPcf8574 true when PCF8574 should be used
     * @return
     */
    public I2cSerialCharLcdBuilder isPcf8574(boolean isPcf8574) {
      this.isPcf8574 = isPcf8574;
      return this;

    }

    /**
     * Specify which pin of the PCF8574 is the LCD Enable pin
     * @param e1Pin enable pin [0:7]
     * @return
     */
    public I2cSerialCharLcdBuilder e(int e1Pin) {
      this.e1Pin = e1Pin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 is the second Enable pin. Large
     * LCD displays, more than 80 characters have 2 enable pins, one for
     * the first 'half' of the display, one for the second.
     * @param e2Pin Second anable pin [0:7]
     * @return
     */
    public I2cSerialCharLcdBuilder e2(int e2Pin) {
      this.e2Pin = e2Pin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 is the LCD Register Select pin.
     * @param rsPin rs pin [0:7]
     * @return
     */
    public I2cSerialCharLcdBuilder rs(int rsPin) {
      this.rsPin = rsPin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 is the Read/Write pin
     * @param rwPin rw pin [0:7]
     * @return
     */
    public I2cSerialCharLcdBuilder rw(int rwPin) {
      this.rwPin = rwPin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 controls the LCD backlight. The LCM1602
     * uses one of the PCF8574 pins to control the LCD backlight. Some modules use
     * another means of driving the LCD backlight and use this pin for the second
     * LCD enable pin on large LCDs. If your module uses the PCF8574 to control
     * the backlight specify it here. If it is not specified {@link #enableBackLight(boolean)} has no effect.
     * @param blPin backlight control pin [0:7]
     * @return
     */
    public I2cSerialCharLcdBuilder bl(int blPin) {
      this.blPin = blPin;
      hasBackLight = true;
      return this;
    }

    /**
     * specify which pins of the PCF8574 are connected to the LCD data lines D4-D7
     * @param d4Pin LCD D4 pin
     * @param d5Pin LCD D5 pin
     * @param d6Pin LCD D6 pin
     * @param d7Pin LCD D7 pin
     * @return
     */
    public I2cSerialCharLcdBuilder data(int d4Pin, int d5Pin, int d6Pin, int d7Pin) {
      this.d4Pin = d4Pin;
      this.d5Pin = d5Pin;
      this.d6Pin = d6Pin;
      this.d7Pin = d7Pin;
      return this;
    }

    /**
     * Specify the I2C address of the PCF8574. This is the value of A0-A2 pins, not
     * the base address of the PCF8574 which is inferred with {@link #isPcf8574(boolean)}
     * @param address value of A0-A2 [1:7]
     * @return
     */
    public I2cSerialCharLcdBuilder address(int address) {
      this.address = address;
      return this;
    }

    /**
     * Specify the name of the I2C bus that the LCD is connected to
     * @param bus the name of the bus returned from {@link PeripheralManagerService#getI2cBusList()}
     * @return the builder
     */
    public I2cSerialCharLcdBuilder withBus(String bus) {
      this.bus = bus;
      return this;
    }

    /**
     * Build the LCD
     * @return A new {@link #I2cSerialCharLcd(int, int, int, int, int, int, int, int, int, int, int, boolean, boolean, int, String)} with your pin mapping.
     */
    public I2cSerialCharLcd build() {
      return new I2cSerialCharLcd(width, height, address, e1Pin, e2Pin, rsPin, rwPin,
              d4Pin, d5Pin, d6Pin, d7Pin, isPcf8574, hasBackLight, blPin, bus);
    }
  }
}
