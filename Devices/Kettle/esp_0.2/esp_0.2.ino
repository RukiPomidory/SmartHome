#include "ESP8266WiFi.h"

unsigned long start = 0;

WiFiServer server(3333);

void setup() 
{
    Serial.begin(115200);
    server.begin();
    
    WiFi.mode(WIFI_AP);

    WiFi.softAPConfig(IPAddress(192,168,42,1), IPAddress(192,168,42,1), IPAddress(255,255,255,0));
    WiFi.softAP("SMART_T");
    
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
            char cmd = client.read();
            processCommand(cmd);
        }
    }
    else
    {
        client = server.available();
    }

    if (Serial.available())
    {
        char c = Serial.read();
        client.print(c);
    }
}

void processCommand(char cmd)
{
    // Выбираем команду
    switch(cmd)
    {
        // Получение SSID и пароля сетки, к которой надо подключиться
        case 'a':
        case 'A':
            connectToAccessPoint();
            break;

        case 'i':
        case 'I':
            sendIp();
            break;
            
        default:
            redirect(cmd);
            return; 
    }
}

void sendIp()
{   
    String ip = WiFi.softAPIP().toString();
    client.println(ip);
}

//void sendData(byte* data)
//{
//    sendData(data, sizeof(data) / (sizeof(data[0])));
//}

void sendData(String data)
{
    data.concat(";\n");
    client.print(data);
    client.flush();
    
    delay(10);
}

void redirect(char cmd)
{
    Serial.println((int)cmd);
    
}

void Error(byte id)
{
    byte data[] = {'E', id};
    //sendData(data);
    delay(20);
}

//
// _____ ВНИМАНИЕ! ___ ВЕСЬ КОД НИЖЕ ПЕРЕНЕСЕН И НЕ ЯВЛЯЕТСЯ РЕШЕНИЕМ ____
//


void connectToAccessPoint()
{
    byte ssidLength = Serial.read();
    char ssid[ssidLength + 4]; // берем с запасом для кавычек, запятой и символа конца строки

    ssid[0] = '"';
    
    for (byte i = 0; i < ssidLength; i++)
    {
        ssid[i + 1] = Serial.read();
    }
    ssid[ssidLength + 1] = '"';
    ssid[ssidLength + 2] = ',';
    ssid[ssidLength + 3] = 0;
    
    byte assertion = Serial.read();
    if (assertion != 0)
    {
        Error(13);
        return;
    }

    byte passLength = Serial.read();

    char password[passLength + 3];
    password[0] = '"';
    password[passLength + 1] = '"';

    // Принудительно выставляем конец строки в стиле C, потому что без этого не работает
    password[passLength + 2] = 0;
    
    for (byte i = 0; i < passLength; i++)
    {
        password[i + 1] = Serial.read();
    }

    assertion = Serial.read();
    if (assertion != 0)
    {
        Error(13);
        return;
    }
}
