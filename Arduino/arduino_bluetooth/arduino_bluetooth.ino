#include <Servo.h>

#define GREEN 13
#define YELLOW 8

Servo grnd,arm_1,arm_2,grip;

float value[4];
String data="";

void setup() {
  pinMode(GREEN, OUTPUT);
  pinMode(YELLOW, OUTPUT);
  
  grip.attach(9);
  arm_2.attach(10);
  arm_1.attach(11);
  grnd.attach(12);

  grip.write(115); // 115 - 175
  arm_2.write(30); // 30 - 120
  arm_1.write(180); // 80 - 180
  grnd.write(90); // 0 - 180
  
  Serial.begin(9600);

  //give the Robotic Arm some time
    for(int i = 0; i < 10; i++){
      Serial.print(".");
      delay(1000);
      }

    digitalWrite(GREEN, HIGH);
    delay(500);
  
}
 
void loop() {
  int n=0;
  char character;
  while(Serial.available() > 0 && n!=4){
    digitalWrite(YELLOW, HIGH);
    character = Serial.read();
    if (character == '\n')
    {
      value[n]=data.toFloat();
      Serial.print(n);
      Serial.print(" ");
      Serial.println(value[n]);
      data = "";
      //character=0;
      n=n+1;
    }
    data=data+character;
    digitalWrite(YELLOW, LOW);
  }
  
  //write values
  if(10<=value[0] && value[0]<=170){
    grnd.write(value[0]);
  }
  if(80<=value[1] && value[1]<=180){
    arm_1.write(value[1]);
  }
  if(30<=value[2] && value[2]<=120){
    arm_2.write(value[2]);
  }
  if(115<=value[3] && value[3]<=175){
    grip.write(value[3]);
  }

  /*
  if(value[1]<80){
    arm_2.write(value[2]);
  }
  */
  
  delay(300);
}
