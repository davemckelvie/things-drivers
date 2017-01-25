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
package nz.geek.android.things.drivers;

/**
 * Interface for an 8 bit IO port see {@link nz.geek.android.things.drivers.i2c.Pcf8574} for implementation
 */
public interface IoPort {

  /**
   * write a 'byte' to the port. This takes an {@link int} the LSB of the int is used.
   * @param mask data does not affect port state when mask bit is 1
   * @param data the data to write to the port
   * @return true when the data is written successfully
   */
  boolean writeByte(int mask, int data);

  /**
   * Read the last value written to the port.
   * @return last value written to the port (LSB is value)
   */
  int readValue();

  /**
   * set the given port pin to the given value
   * @param pin the pin to set [0:7]
   * @param state true to set it, false to clear it
   */
  void setPin(int pin, boolean state);

  /**
   * Get the state of the given port pin
   * @param pin the pin to get [0:7]
   * @return true when set (logic 1), false when clear (logic 0)
   */
  boolean getPin(int pin);

  /**
   * Read the port data, the state of all pins.
   * @return the byte read from the port (as an int LSB is port data)
   */
  int readByte();

  /**
   * Close the port. The port is invalid after being closed.
   */
  void close();
}
