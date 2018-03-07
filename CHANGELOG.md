## 1.7.0 (2018-03-07)

  - updated for Android Things developer preview 0.7

## 1.3.0 (2017-09-21)

  - refactor Lcd interface to CharacterDisplay
  - implement LedCharacterDisplay

## 1.2.4 (2017-01-25)

  - add getPin() and readByte() to pcf8574

## 1.2.3 (2017-01-18)

  - add lux user sensor driver using TCS34725

## 1.2.2 (2017-01-15)

  - bugfix: initialise CGRAM on 4004 LCD displays [issue 8](https://github.com/davemckelvie/things-drivers/issues/8)

## 1.2.1 (2017-01-14)

  - bugfix: update control mask when switching display

## 1.2.0 (2017-01-14)

 - add TCS34725 colour sensor

## 1.1.2 (2017-01-10)

 - add methods to give I2C bus to ADC and LCD builders
 - the end of the binary version numbering (I thought it was funny)

## 1.1.1 (2017-01-04)

 - Added I2C Buttons

## 1.1.0 (2017-01-01)

 - Added PCF8591 ADC

## 1.0.1 (2016-12-28)

 - Bugfix: backlight disabled when CGRAM written to

## 1.0.0 (2016-12-28)

 - Support custom character creation with CGRAM

## 0.1.1 (2016-12-27)

 - Added support for large displays e.g., 40x4

## 0.1.0 (2016-12-25)

 - Added LCD builder to initialise address and pin mapping in
 - Removed some hard coded values
 - changed `print()` method to accept line number instead of DDRAM address

## 0.0.1 (2016-12-23)

 - Initial release.
 - Basic PCF8574 functionality
 - Basic LCD functionality with hard coded address and pin mapping
 - Briefly described in http://android.geek.nz/?p=68
