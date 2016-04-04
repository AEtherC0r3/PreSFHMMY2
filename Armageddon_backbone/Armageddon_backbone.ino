#include <OneWire.h>
#include <Wire.h>
#include "Adafruit_SI1145.h"

int check_pulse();
int get_bpm();
float get_temp();
float get_uv();

OneWire temp_sensor(7);
Adafruit_SI1145 uv_sensor = Adafruit_SI1145();
long int time_new, time_old;
bool count = false;

void setup() {
  Serial.begin(9600);
  //Serial.print("AT+NAMEHardCode Wizards");
  //Serial.print("AT+PIN1337");
  
  uv_sensor.begin();
   
  time_new = millis();
}

void loop() {
  check_pulse();
  
  if (Serial.available() > 0) {
    switch((char)Serial.read()) {
      case 'p':
        Serial.print('p');
        Serial.print(get_bpm());
        Serial.print('p');
        break;
      case 't':
      {
        float tmp = get_temp();
        Serial.print('t');
        Serial.print(tmp);
        Serial.print('t');
        break;
      }
      case 'u':
        Serial.print('u');
        Serial.print(get_uv());
        Serial.print('u');
        break;
    }
  }
}

int check_pulse() {
  if(count != true && digitalRead(5) == HIGH)  {
    count = true;
    time_old = time_new;
    time_new = millis();
  }
  else if (digitalRead(5) == LOW)
    count = false;
}

int get_bpm() {
  int t = time_new - time_old;
  return 60000 / t;
}

float get_temp()  {
  byte data[12];
  byte address[8];
  
  temp_sensor.search(address);
  temp_sensor.reset();
  temp_sensor.select(address);
  temp_sensor.write(0x44, 1);
  delay(1000);
  temp_sensor.reset();
  temp_sensor.select(address);
  temp_sensor.write(0xBE);
  
  for (byte i = 0; i < 12; i++) {
    data[i] = temp_sensor.read();
  }

  int16_t raw = (data[1] << 8) | data[0];

  float celsius = (float)raw / 16.0;

  return celsius;
}

float get_uv()  {
  float uv_index = uv_sensor.readUV();
  uv_index /= 100.0;
  return uv_index;
}


