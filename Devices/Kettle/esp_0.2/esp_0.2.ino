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
            char cmd = client.read();
            processCommand(cmd);
        }
    }
    else
    {
        client = server.available();
    }
}

void processCommand(char cmd)
{
    // Выбираем команду
    switch(cmd)
    {
        // Включение
        case 'h':
        case 'H':
            on();
            break;
        
        // Выключение
        case 'k':
        case 'K':
            off();
            break;

        // Запрос значения с датчика
        case 'r':
        case 'R':
            sendSensorData();
            break;

        // Вкл/Выкл потока информации с датчиков
        case 'f':
        case 'F':
            flowHandler();
            break;

        // Получение SSID и пароля сетки, к которой надо подключиться
        case 'a':
        case 'A':
            connectToAccessPoint();
            break;

        case 'i':
        case 'I':
            sendIp();
            break;

        case 'l':
        case 'L':
            setLowLevel();
            break;

        case 'u':
        case 'U':
            setUpLevel();
            calibrate();
            break;
        
        default:
            Error(11);
            swSerial.println("err " + cmd);
            return;
    }
}

