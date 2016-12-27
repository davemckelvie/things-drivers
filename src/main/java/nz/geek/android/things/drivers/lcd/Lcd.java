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

public interface Lcd {
  /**
   * Connect to the LCD display. This initialises the display.
   */
  void connect();

  /**
   * Disconnect from the display
   */
  void disconnect();

  /**
   * Print a message to the display starting at the given display line
   * @param line  the line to print the message [1:height]
   * @param message the message to print
   */
  void print(int line, String message);

  /**
   * Switch on and off the LCD backlight
   * @param enable true to enable
   */
  void enableBackLight(boolean enable);

  /**
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
  void setCgRam(int address, byte[] pattern);

  /**
   * Clear a given line of the LCD display
   * @param line line to clear [1:height]
   */
  void clearLine(int line);

  /**
   * clear the display. Fills DDRAM with space character (0x20)
   */
  void clearDisplay();

  /**
   * Get the number of LCD character rows
   * @return LCD rows
   */
  int getWidth();

  /**
   * Get the number of LCD character columns
   * @return LCD columns
   */
  int getHeight();
}
