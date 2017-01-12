/*
 * Copyright 2017 Dave McKelvie
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
package nz.geek.android.things.drivers.adc;

public interface Adc {
  /**
   * Start the ADC conversion process
   */
  void startConversions();

  /**
   * stop conversions
   */
  void stopConversions();

  /**
   * Non-blocking read of an ADC channel, will return the
   * latest valid ADC reading.
   * @param channel channel to read
   * @return last read channel value
   */
  int readChannel(int channel);

  /**
   * clean up any threads
   */
  void close();
}
