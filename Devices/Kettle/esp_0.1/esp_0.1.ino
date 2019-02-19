#include "ESP8266WiFi.h"

unsigned long start = 0;

WiFiServer server(3333);

void setup() 
{
    Serial.begin(115200);
    server.begin();
    
    WiFi.mode(WIFI_AP);
    //WiFi.begin("SMART", "");

    WiFi.softAPConfig(IPAddress(192,168,42,1), IPAddress(192,168,42,1), IPAddress(255,255,255,0));
    WiFi.softAP("SMART");
    
    delay(100);

    Serial.println("Setup done");
    Serial.println(WiFi.softAPIP());
}

WiFiClient client;

void loop() 
{
    if (client.connected())
    {
        
        if (client.available() > 0)
        {
            Serial.write(client.read());
        }
    }
    else
    {
        client = server.available();
    }
    
    if (millis() - start > 5000)
    {
        start = millis();
        Serial.print(client.connected());
        Serial.print(' ');
        Serial.println(start);
    }
}
