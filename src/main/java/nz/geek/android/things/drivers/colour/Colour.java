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
package nz.geek.android.things.drivers.colour;

public class Colour {
  private static final int RED_INDEX = 0;
  private static final int GREEN_INDEX = 1;
  private static final int BLUE_INDEX = 2;
  private static final int CLEAR_INDEX = 4;
  private final int[] raw;

  public Colour(int[] raw) {
    this.raw = raw;
  }

  public Colour(int red, int green, int blue, int clear) {
    raw = new int[]{red, green, blue, clear};
  }

  public int[] getRaw() {
    return raw;
  }

  public int getRedRaw() {
    if (raw.length == 0) return 0;
    return raw[RED_INDEX];
  }

  /**
   * Calculate lux (Illuminance) for the given RGB values
   * @param red sensor value
   * @param green sensor value
   * @param blue sensor value
   * @return lux
   */
  public static int toLux(int red, int green, int blue) {
    return (int) ((-0.32466f * (float) red) + (1.57837f * (float) green) + (-0.73191f * (float) blue));
  }

  private int toLux(int[] raw) {
    return toLux(raw[RED_INDEX], raw[GREEN_INDEX], raw[BLUE_INDEX]);
  }

  /**
   * Calculate the Correlated Colour Temperature (CCT) from the given
   * RGB values. Formula taken from http://ams.com/eng/content/view/download/145158
   * (TAOS Design Note 25 DN25)
   * @param red value from sensor
   * @param green value from sensor
   * @param blue value from sensor
   * @return CCT
   */
  public static int toColourTemperature(int red, int green, int blue) {
    float X, Y, Z, xc, yc, n;

    X = (-0.14282f * red) + (1.54924f * green) + (-0.95641f * blue);
    Y = (-0.32466f * red) + (1.57837f * green) + (-0.73191f * blue);
    Z = (-0.68202f * red) + (0.77073f * green) + ( 0.56332f * blue);

    xc = (X) / (X + Y + Z);
    yc = (Y) / (X + Y + Z);

    n = (xc - 0.3320F) / (0.1858F - yc);

    return (int) ((449.0F * Math.pow(n, 3)) + (3525.0F * Math.pow(n, 2)) + (6823.3F * n) + 5520.33F);
  }

  private int toColourTemperature(int[] raw) {
    if (raw == null || raw.length < 3) return -1;
    return toColourTemperature(raw[RED_INDEX], raw[GREEN_INDEX], raw[BLUE_INDEX]);
  }
}
