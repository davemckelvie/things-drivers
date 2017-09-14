package nz.geek.android.things.drivers.lcd;

import nz.geek.android.things.drivers.display.CharacterDisplay;


public abstract class AbstractCharacterDisplay implements CharacterDisplay {

  protected static final int SPACE = 0x20;

  protected static final int LCD_BF = 0x80; // D7

  /**
   * DDRAM addresses for start of lines 1 and 2
   */
  public static final int  LCD_LINE_ONE = 0x00;
  public static final int  LCD_LINE_TWO = 0x40;

  /**
   * Commands used with {@link #writeCommand(int)}
   */
  protected static final int LCD_CLEAR_DISPLAY = 0x01;
  protected static final int LCD_RETURN_HOME = 0x02;
  protected static final int LCD_DECREMENT_DD_RAM = 0x04;
  protected static final int LCD_INCREMENT_DD_RAM = 0x06;
  protected static final int LCD_NO_SHIFT = 0x04;
  protected static final int LCD_SHIFT = 0x05;
  protected static final int LCD_DISPLAY_ON = 0x0C;
  protected static final int LCD_DISPLAY_OFF = 0x08;
  protected static final int LCD_CURSOR_ON = 0x0A;
  protected static final int LCD_CURSOR_OFF = 0x08;
  protected static final int LCD_BLINK_ON = 0x09;
  protected static final int LCD_BLINK_OFF = 0x08;
  protected static final int LCD_SHIFT_CURSOR_LEFT = 0x10;
  protected static final int LCD_SHIFT_CURSOR_RIGHT = 0x14;
  protected static final int LCD_SHIFT_DISPLAY_LEFT = 0x18;
  protected static final int LCD_SHIFT_DISPLAY_RIGHT = 0x1C;
  protected static final int LCD_8_BIT = 0x30;
  protected static final int LCD_4_BIT = 0x20;
  protected static final int LCD_2_LINE = 0x28;
  protected static final int LCD_1_LINE = 0x20;
  protected static final int LCD_5_10_DOTS = 0x24;
  protected static final int LCD_5_7_DOTS = 0x20;
  protected static final int LCD_SET_CG_RAM = 0x40;
  protected static final int LCD_SET_DD_RAM = 0x80;

  protected final int width;
  protected final int height;
  protected final boolean hasBackLight;

  protected boolean doubleWrite = false;

  protected AbstractCharacterDisplay(int width, int height, boolean hasBackLight) {
    this.width = width;
    this.height = height;
    this.hasBackLight = hasBackLight;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  protected boolean isDoubleDisplay() {
    return (width * height > 80);
  }

  protected abstract void write(int data);
  protected abstract void writeCommand(int command);
  protected abstract void switchDisplay(int display);

  protected void init() {

    switchDisplay(1);
    initialiseLcd();
    if (isDoubleDisplay()) {
      switchDisplay(2);
      initialiseLcd();
      switchDisplay(1);
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
   * convert display line number to DDRAM address
   * @param line line number [1:height]
   * @return DDRAM address for the start of line
   */
  protected int lineToAddress(int line) {
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

  protected void initialiseLcd() {

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
  public boolean hasBackLight() {
    return hasBackLight;
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
  public int setCustomCharacter(byte[] pattern) {
    // TODO: 12/09/17
    return 0;
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

}
