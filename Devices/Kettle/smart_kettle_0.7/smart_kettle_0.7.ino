#include "Sensor.cpp"
#include "Settings.h"
#include <SoftwareSerial.h> // позволяет создать еще один UART-порт на любых пинах ардуино
#include <EEPROM.h> // Долговременная память

#define PRESS_SENSOR_F A0 // Передний   \|
#define PRESS_SENSOR_L A3 // Левый      || Датчики давления
#define PRESS_SENSOR_R A2 // Правый     /|

#define TEMP_SENSOR A1  // Датчик температуры
#define RELAY 4         // Реле

// Порты SoftwareSerial порта
#define swRX 6
#define swTX 7


// Идет ли сейчас нагрев
bool heating = false;

// Контроль нескончаемого потока данных с датчиков
bool flow = false;

// Массив для обработки входящих данных
char buff[4];

// ID клиента сервера. Обычно это 0
int id = 0;

// Для измерения временных промежутков
unsigned long start;

// Для определения бездействия модуля
unsigned long lastData;

// Количество оставшихся байтов в сообщении
int bytesLeft = 0;


int lowWaterValues[3]; // Значения ацп при нулевом уровне воды. Для калибровки
int highWaterValues[3]; // Значения при уровне 2л

// -------------- Функции --------------
// Включение и выключение нагревателя
void on(bool force = false); // Проверяет уровень воды и если все ок, включает нагрев. 
							// При force=true проверка игнорируется
void off();

// Получение температуры
float getTemperature();

// Получение количества воды
float getWaterAmount();

// Отправка значения с указанного датчика
void sendSensorData();

// Отправка сообщения о конкретной ошибке
void Error(byte);
//--^--^--^--^--^--^--^--^--^--^--^--^--

// Создаем последовательный порт на выбранных пинах
SoftwareSerial swSerial(swRX, swTX);

// Создаем датчики
Sensor temperatureSensor(temperatureSensorAlpha, TEMP_SENSOR);
Sensor pressureSensorF(pressureSensorAlphaF, PRESS_SENSOR_F);
Sensor pressureSensorR(pressureSensorAlphaR, PRESS_SENSOR_R);
Sensor pressureSensorL(pressureSensorAlphaL, PRESS_SENSOR_L);

Config config;

void setup() 
{
    // Светодиод для дебага
    pinMode(LED_BUILTIN, OUTPUT);
    digitalWrite(LED_BUILTIN, LOW);
    
    // Выставляем режим OUTPUT на реле и сразу отключаем
    pinMode(RELAY, OUTPUT);
    digitalWrite(RELAY, HIGH);

    // Запускаем последовательный порт
    Serial.begin(115200);
    swSerial.begin(38400);
    delay(500);

    // Запуск сервера на только что запущенном ESP8266
    setupServer();

    start = millis();
    lastData = millis();

    // TODO:
    //  здесь еще можно прочитать входные данные и определить
    //  успешность запуска, но это в следующей версии

    EEPROM.get(0, config);
    if (config.kF == 0 && config.kL == 0 && config.kR == 0)
    {
        initDefaultConfig(config);
    }
}

void loop() 
{
    float temperature;
    float waterAmount;
    
    if (millis() - start > 50)
    {
        // Получаем значения температуры и уровня воды
        temperature = getTemperature();
        waterAmount = getWaterAmount();
        start = millis();
    }

    // Отключаемся при достижении максимальной температуры
    if(round(temperature) >= maxTemperature && heating)
    {
        off();
        sendData('D');
    }

    // Обработка входящих сообщений
    if(Serial.available())
    {
        lastData = millis();
        digitalWrite(LED_BUILTIN, HIGH);
        // Проверяем на наличие данных
        int state = detectInputData();
        if(1 == state)
        {
            // Здесь мы читаем запятую, которая
            // разделяет команду и следующую цифру
            char c = Serial.read();
            
            bytesLeft = 0;
            id = 0;
                
            // Читаем id подключенного клиента.
            // Вряд ли в первых версиях к серверу будет подключаться
            // больше 9 пользователей, так что пока
            // работаем только с одной цифрой
            c = Serial.read();
            id = c - '0';
            c = Serial.read();
            if (c != ',' || id < 0 || id > 9)
            {
                return;
            }

            // А здесь мы принимаем первую цифру длины данных.
            // Вот тут уже спокойно может оказаться число
            // больше 9, так что однозначно делаем цикл
            c = Serial.read();
            while(c != ':')
            {
                bytesLeft *= 10;
                bytesLeft += c - '0';
                
                c = Serial.read();
            }

            // Вот мы и добрались до заветных байтов команды.
            // Можно их прочитать и обработать
            char cmd = Serial.read();
            bytesLeft--;
            processCommand(cmd);
        }
        else if (2 == state)    // state "FAILED"
        {
            Error(14);
        }
        else if (3 == state)    // state "WIFI GOT"
        {
            sendIp();
        }
        
    }
    else
    {
        digitalWrite(LED_BUILTIN, LOW);
    }

    if (millis() - lastData > 60000)
    {
        Serial.println("AT+RST");
        delay(1000);
        setupServer();
        lastData = millis();
    }
    
    delay(5);
}

void setupServer()
{
    Serial.println("ATE0"); // отрубаем echo
    delay(5);
    Serial.println("AT+CWMODE=3");  // включаем оба режима AP и STA
    delay(5);
    Serial.println("AT+CIPMUX=1");  // разрешаем множественное подключение
    delay(5);
    Serial.println("AT+CIPSERVER=1,3333");  // запускаем сервер
    delay(10);
}

int detectInputData()
{
    // Выходные значения:
    // 0 - данные не распознаны
    // 1 - "+IPD" - пришли новые данные
    // 2 - "FAIL" - не удалось подключиться к точке доступа
    // 3 - "WIFI GOT IP" - удачное подключение к точке доступа

    char c = Serial.read();
    switch(c)
    {
        case '+':
            c = Serial.read();
            if ('I' != c) return 0;
            c = Serial.read();
            if ('P' != c) return 0;
            c = Serial.read();
            if ('D' != c) return 0;
            return 1;
            
        case 'F':
            c = Serial.read();
            if ('A' != c) return 0;
            c = Serial.read();
            if ('I' != c) return 0;
            c = Serial.read();
            if ('L' != c) return 0;
            return 2;

        case 'W':
            c = Serial.read();
            if ('I' != c) return 0;
            c = Serial.read(); 
            if ('F' != c) return 0;
            c = Serial.read();
            if ('I' != c) return 0;
            c = Serial.read();
            if (' ' != c) return 0;
            c = Serial.read();
            if ('G' != c) return 0;
            c = Serial.read();
            if ('O' != c) return 0;
            c = Serial.read();
            if ('T' != c) return 0;
            return 3;

        default:
            return 0;
    }
    
    if ('+' != c)
    {
        return false;
    }
    
    return 0;
}

void processCommand(char cmd)
{
    for (;;)
    {
        // Выбираем команду
        switch(cmd)
        {
            // Включение
            case 'H':
                on();
                break;
            
            // Выключение
            case 'K':
                off();
                break;
    
            // Запрос значения с датчика
            case 'R':
                sendSensorData();
                break;
    
            // Вкл/Выкл потока информации с датчиков
            case 'F':
                flowHandler();
                break;

            // Получение SSID и пароля сетки, к которой надо подключиться
            case 'A':
                connectToAccessPoint();
                break;

            case 'I':
                sendIp();
                break;

            case 'L':
                setLowLevel();
                break;

            case 'U':
                setUpLevel();
                calibrate();
                break;
            
            default:
                Error(11);
                swSerial.println("err " + cmd);
                return;
        }

        // Смотрим на оставшиеся биты и решаем, пора ли выходить
        bytesLeft--;
        if(bytesLeft < 0)
        {
            swSerial.println("ok ");
            return;
        }
        else
        {
            swSerial.println(bytesLeft);
        }

        // Счетчик не выгоняет, еще можно поиграть.
        // Читаем следующий бит
        cmd = Serial.read();
    }
    swSerial.print("impossible");
}

void on(bool force = false)
{
    if(!force)
    {
        // Проверка уровня воды
        float water = getWaterAmount();
        if(water < minWaterAmount)
        {
            Error(1);
            return;
        }
        else if(water > maxWaterAmount)
        {
            Error(2);
            return;
        }

        int temperature = round(getTemperature());
        if (temperature >= maxTemperature)
        {
            off();
            sendData('D');
            return;
        }
    }
    
    // Замыкаем реле
    digitalWrite(RELAY, LOW);
    
    // Нагрев пошел
    heating = true;

    // Отправляем подтверждение включения
    sendData('H');
}

void off()
{
    // Размыкаем реле
    digitalWrite(RELAY, HIGH);
    
    // Нагрев прекращен
    heating = false;

    // Отправляем подтверждение выключения
    sendData('K');
}

float getTemperature()
{
    // Получаем значение с АЦП и ждем
    float value = temperatureSensor.getValue();
    delay(10);
    
    // Вычисляем текущее сопротивление термистора
    float resistance = balanceR * (1023.0 / value - 1);

    // Получаем температуру в кельвинах, 
    // используя перестроенное бета-уравнение
    float tKelvin = (beta * room_t0) / 
            (beta + (room_t0 * log(resistance / defaultR)));

    // Температура в градусах цельсия
    float tempC = tKelvin - 273.15;
            
    //Возвращаем температуру в градусах цельсия
    return tempC;
}

float getWaterAmount()
{
    // Получаем значения с датчиков
    float valueF = pressureSensorF.getValue();
    float valueR = pressureSensorR.getValue();
    float valueL = pressureSensorL.getValue();

    // Считаем уровень воды отдельно для каждого
    float amountF = (valueF - config.biasF) * config.kF;
    float amountR = (valueR - config.biasR) * config.kR;
    float amountL = (valueL - config.biasL) * config.kL;

    // Берем среднее арифметическое
    float amount = (amountF + amountR + amountL) / 3;

    // Убираем отрицательные значения
    if(amount < 0)
    {
        amount = 0;
    }

    // Домножаем на общий коэффициент и возвращаем
    return amount;
}

void sendSensorData()
{
    byte id = Serial.read();
    bytesLeft--;
    
    int data;
    switch (id)
    {
        // Состояние нагревателя
        case 0:
            data = (int) heating;
            break;

        // Датчики давления
        case 1:
            data = pressureSensorF.getValue();
            break;
        case 2:
            data = pressureSensorL.getValue();
            break;
        case 3:
            data = pressureSensorR.getValue();
            break;

        // Термистор
        case 4:
            data = temperatureSensor.getValue();
            break;

        // Уровень воды
        case 5:
            data = getWaterAmount() * 10;
            break;

        // Температура
        case 6:
            data = round(getTemperature());
            break;

        default:
            Error(10);
            return;
    }

    byte buf[4];
    buf[0] = 'T';
    buf[1] = id;
    
    if (data > 256)
    {
        buf[2] = data / 256;
        buf[3] = data % 256;
        sendData(buf, 4);
    }
    else
    {
        buf[2] = data;
        sendData(buf, 3);  
    }
}

void flowHandler() //Не знаю, зачем я его сделал, но пусть будет
{
    byte mode = Serial.read();
    bytesLeft--;
    
    if (1 == mode)
    {
        flow = true;
    }
    else if (0 == mode)
    {
        flow = false;
    }
    else
    {
        Error(10);
    }
}

void sendData(char character)
{
    byte data[] = {character, ';', '\n'};
    Serial.println("AT+CIPSEND=" + String(id) + ",2");

    // Ждем у моря погоды
    delay(100);
    
    Serial.write(data, 3);
    Serial.flush();
    delay(5);
}

void sendData(byte* data)
{
    sendData(data, sizeof(data) / (sizeof(data[0])));
}

void sendData(byte* data, int length)
{
    Serial.println("AT+CIPSEND=" + String(id) + ',' + String(length + 1));
    
    // Ждем у моря погоды
    delay(5);
    
    Serial.write(data, length);
    Serial.write(';');
    Serial.write('\n');
    Serial.flush();
    delay(10);
}

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

void sendIp()
{
    // Чтобы получить OK даже будучи сферическим конем в вакууме, 
    // отправляем дефолтную команду (потому что запросить IP можно и командой
    // извне просто так, без установления связи и получения "OK" до этого)
    // Заодно проверяем, готов ли модуль исполнять наши команды
    Serial.println("AT");
    
    start = millis();
    bool firstRead = false; // Запоминаем, была ли прочитана первая буква сообщения "OK"
    
    for(;;)
    {
        if (Serial.available())
        {
            char c = Serial.read();
            if (firstRead)
            {
                if (c == 'K')
                {
                    break;
                }
            }
            else
            {
                if (c == 'O')
                {
                    firstRead = true;
                }
            }
        }
        if(millis() - start > 3000)
        {
            break;
        }
    }

    Serial.println("AT+CIPSTA?");   // Запрашиваем данные 
    
    char target[] = "+CIPSTA:ip:";  // Строка, которую мы ищем
    start = millis();   // Инициализируем счетчик времени

    bool got = false;
    while (millis() - start < 100) // Лимит ожидания - 100мс
    {
        if (Serial.available() >= sizeof(target)/sizeof(target[0]))
        {
            got = true;
            
            // Берем размер на 1 меньше, т.к. в длину массива входит символ окончания строки
            for (int i = 0; i < sizeof(target)/sizeof(target[0]) - 1; i++)
            {
                // Проверяем посимвольно совпадение с эталоном
                char c = Serial.read();
                if (c != target[i])
                {
                    got = false;
                    break;
                }
            }
        }
        
        if (got)
        {
            break;
        }
    }
    
    if (got)
    {
        int counter = 2; // Считает действительное количество символов в массиве ip
        char ip[17]; // 255:255:255:255 - 12 + 3 символа, + место для первых двух символов "IP"
        ip[0] = 'I';
        ip[1] = 'P';

        // Ждем, пока прогрузятся все символы, чтобы не нахватать мусора
        while (Serial.available() < 15) { delay(1); }
        delay(10);
        
        char c = Serial.read(); // Проверяем, что сейчас действительно пойдет IP-адрес
        if (c != '"')
        {
            Error(15);  // Код ошибки получения IP-адреса
            return;
        }

        // Начинаем читать сам IP и заканчиваем, когда натыкаемся
        // на вторую кавычку или выходим за границы разумного
        c = Serial.read();
        while (c != '"' && counter < 17)
        {
            ip[counter] = c;
            counter++;
            c = Serial.read();
            delay(5); // Проверка: влияет ли задержка на читаемые данные
        }
        sendData(ip, counter);
    }
    else
    {
        Error(16);  // Код ошибки лимита времени ожидания
    }
}

void setUpLevel()
{
    int front = analogRead(PRESS_SENSOR_F);
    int right = analogRead(PRESS_SENSOR_R);
    int left = analogRead(PRESS_SENSOR_L);

    highWaterValues[0] = front;
    highWaterValues[1] = right;
    highWaterValues[2] = left;

    sendData('O');
}

void setLowLevel()
{
    int front = analogRead(PRESS_SENSOR_F);
    int right = analogRead(PRESS_SENSOR_R);
    int left = analogRead(PRESS_SENSOR_L);
    
    //lowWaterValues = {front, right, left};
    lowWaterValues[0] = front;
    lowWaterValues[1] = right;
    lowWaterValues[2] = left;

    sendData('O');
}

void calibrate()
{
    config.biasF = lowWaterValues[0];
    config.biasR = lowWaterValues[1];
    config.biasL = lowWaterValues[2];

    config.kF = 2.0 / (highWaterValues[0] - lowWaterValues[0]);
    config.kR = 2.0 / (highWaterValues[1] - lowWaterValues[1]);
    config.kL = 2.0 / (highWaterValues[2] - lowWaterValues[2]);

    EEPROM.put(0, config);
}

void initDefaultConfig(Config conf)
{
    conf.biasF = biasF;
    conf.biasR = biasR;
    conf.biasL = biasL;

    conf.kF = kF;
    conf.kR = kR;
    conf.kL = kL;
}

void Error(byte id)
{
    byte data[] = {'E', id};
    sendData(data);
    delay(20);
}
