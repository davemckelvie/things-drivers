package nz.geek.android.things.drivers.led;

import java.nio.ByteBuffer;
import java.util.Arrays;

import nz.geek.android.things.drivers.display.CharacterDisplay;
import nz.geek.android.things.drivers.spi.LedMatrixController;

public class LedCharacterDisplay implements CharacterDisplay {

  private LedMatrixController device;
  private final int width;
  private final int height;

  // ascii command characters
  private static final byte STX = 2; // start of text
  private static final byte ETX = 3; // end of text

  // display commands (reusing ascii command characters)
  private static final byte CMD_PRINT_LINE = 0x04;
  private static final byte CMD_CLEAR_LINE = 0x05;
  private static final byte CMD_CLEAR_DISP = 0x06;
  private static final byte CMD_SET_CHARACTER = 0x07;

  // Protocol
  // | start | command | param | data | end |
  // | STX   | 0xXX    | 0xYY  | ...  | ETX |

  public LedCharacterDisplay(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public void connect() {
    if (device == null) {
      device = LedMatrixController.create();
    }
  }

  @Override
  public void disconnect() {
    if (device != null) {
      device.close();
      device = null;
    }
  }

  private byte[] toPacket(int line, String message) {
    ByteBuffer bb = ByteBuffer.allocate(4 + message.length());
    bb.put(STX);
    bb.put(CMD_PRINT_LINE);
    bb.put((byte)(line & 0xFF));
    bb.put(message.getBytes());
    bb.put(ETX);
    return bb.array();
  }

  private byte[] toPacket(byte[] message) {
    ByteBuffer bb = ByteBuffer.allocate(message.length + 2);
    bb.put(STX);
    bb.put(message);
    bb.put(ETX);
    return bb.array();
  }

  @Override
  public void print(int line, String message) {
    device.write(toPacket(line, message));
  }

  @Override
  public void clearLine(int line) {
    byte[] buffer = new byte[2];
    buffer[0] = CMD_CLEAR_LINE;
    buffer[1] = (byte) (line & 0xFF);
    device.write(toPacket(buffer));
  }

  @Override
  public void clearDisplay() {
    byte[] buffer = new byte[]{CMD_CLEAR_DISP};
    device.write(toPacket(buffer));
  }

  @Override
  public void enableBackLight(boolean enable) {
    // no-op
  }

  @Override
  public boolean hasBackLight() {
    return false;
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
  public void setCustomCharacter(int address, byte[] pattern) {
    // TODO: 10/09/17
  }

  @Override
  public void initBarGraph() {
    // TODO: 10/09/17
  }
}
