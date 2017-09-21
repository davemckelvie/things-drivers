/*
 * Copyright 2017 Dave McKelvie <www.android.geek.nz>.
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
   *
   * write a bit pattern for a custom character to the display. What the
   * bit pattern means depends on the display, for an LCD display the
   * bit pattern is defined as:
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
   * @param address the 'address' to write the bit pattern to.
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
   * initialise custom characters for the display of bar graphs
   */
  void initBarGraph();

}
