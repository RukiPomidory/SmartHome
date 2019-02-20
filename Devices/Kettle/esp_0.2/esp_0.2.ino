#include "ESP8266WiFi.h"

unsigned long start = 0;

WiFiServer server(3333);

void setup() 
{
    Serial.begin(115200);
    server.begin();
    
    WiFi.mode(WIFI_AP);

    WiFi.softAPConfig(IPAddress(192,168,42,1), IPAddress(192,168,42,1), IPAddress(255,255,255,0));
    WiFi.softAP("SMART_");
    
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
            redirect('H');
            break;
        
        // Выключение
        case 'k':
        case 'K':
            redirect('K');
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

void sendIp()
{   
    String ip = WiFi.softAPIP().toString();
    client.println(ip);
}

void redirect(char cmd)
{
    Serial.print(cmd);
}

void Error(byte id)
{
    byte data[] = {'E', id};
    sendData(data);
    delay(20);
}

//
// _____ ВНИМАНИЕ! ___ ВЕСЬ КОД НИЖЕ ПЕРЕНЕСЕН И НЕ ЯВЛЯЕТСЯ РЕШЕНИЕМ ____
//


void connectToAccessPoint()
{
    byte ssidLength = Serial.read();
    bytesLeft--;
    char ssid[ssidLength + 4]; // берем с запасом для кавычек, запятой и символа конца строки

    ssid[0] = '"';
    
    for (byte i = 0; i < ssidLength; i++)
    {
        ssid[i + 1] = Serial.read();
        bytesLeft--;
    }
    ssid[ssidLength + 1] = '"';
    ssid[ssidLength + 2] = ',';
    ssid[ssidLength + 3] = 0;
    
    byte assertion = Serial.read();
    bytesLeft--;
    if (assertion != 0)
    {
        Error(13);
        return;
    }

    byte passLength = Serial.read();
    bytesLeft--;

    // Здесь у нас сеть без пароля
    if (0 == passLength) 
    {
        String request = "AT+CWJAP_DEF=" + String(ssid) + "\"\""; //Изменяем параметры подключения к точке доступа: ssid в кавычках, запятая и пустой пароль ""
        Serial.println(request);
        Serial.flush();
        return;
    }

    char password[passLength + 3];
    password[0] = '"';
    password[passLength + 1] = '"';

    // Принудительно выставляем конец строки в стиле C, потому что без этого не работает
    password[passLength + 2] = 0;
    
    for (byte i = 0; i < passLength; i++)
    {
        password[i + 1] = Serial.read();
        bytesLeft--;
    }

    assertion = Serial.read();
    bytesLeft--;
    if (assertion != 0)
    {
        Error(13);
        return;
    }

    String request = "AT+CWJAP_DEF=" + String(ssid) + String(password);
    Serial.println(request);
    Serial.flush();
}
