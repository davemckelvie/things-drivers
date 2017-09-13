package nz.geek.android.things.drivers.display;

/**
 * Interface for a character display
 */
public interface CharacterDisplay {
  /**
   * Connect to the display. This initialises the display.
   */
  void connect();

  /**
   * Disconnect from the display
   */
  void disconnect();

  /**
   * Enable the display
   * @param enable true to enable, false to disable
   */
  void enable(boolean enable);

  /**
   * Print a message to the display starting at the given display line
   * @param line  the line to print the message [1:height]
   * @param message the message to print
   */
  void print(int line, String message);

  /**
   * Clear a given line of the display
   * @param line line to clear [1:height]
   */
  void clearLine(int line);

  /**
   * clear the display. Fills DDRAM with space character (0x20)
   */
  void clearDisplay();

  /**
   * Switch on and off the backlight
   * @param enable true to enable
   */
  void enableBackLight(boolean enable);

  /**
   * Does the display have a back light?
   * @return true if it does
   */
  boolean hasBackLight();

  /**
   * Get the number of display character rows
   * @return rows
   */
  int getWidth();

  /**
   * Get the number of display character columns
   * @return columns
   */
  int getHeight();

  /**
   * OK, it's a bit of a stretch to put this method into the interface,
   * it's not applicable to LED displays (or is it?).
   *
   * write a bit pattern to CGRAM to generate a custom character
   *
   * <pre>
   * bit pattern     eg          hex
   * 76543210
   * ---XXXXX        XXXX        1E
   * ---XXXXX        X   X       11
   * ---XXXXX        X   X       11
   * ---XXXXX        XXXX        1E
   * ---XXXXX        X   X       11
   * ---XXXXX        X   X       11
   * ---XXXXX        XXXX        1E
   * </pre>
   * @param address the CGRAM address to write the bit pattern to.
   * @param pattern the bit pattern to write
   */
  void setCustomCharacter(int address, byte[] pattern);

  /**
   * Variant of {@link #setCustomCharacter(int, byte[])} that returns the address used
   * to store the custom character
   * @param pattern bit pattern of the custom character
   * @return the address used
   */
  int setCustomCharacter(byte[] pattern);

  /**
   * Load bargraph characters into CGRAM
   */
  void initBarGraph();

}
