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
#define swRX 11
#define swTX 12


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

int lowWaterValues[3]; // Значения ацп при нулевом уровне воды. Для калибровки
int highWaterValues[3]; // Значения при уровне 2л

// Таймаут изменения температуры в мс.
// Если температура не растет в течение 
//      этого времени, чайник вскипел.
int tempTimeout = 4000;

// Изменение температуры до этой
// величины не учитываются как изменение
float deltaT = 0.5;

// Текущий достигнутый максимум температуры
float maxTemp;

// Аналогичные параметры для максимальной температуры (вторая проверка)
float maxTempTimeout = 7000;
float deltaMaxT = 0.3;

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

    start = millis();
    lastData = millis();

    // TODO:
    //  здесь еще можно прочитать входные данные и определить
    //  успешность запуска, но это в следующей версии

    EEPROM.get(0, config);
    if (config.kF == 0 || isnan(config.kF))
    {
        initDefaultConfig();
        swSerial.println("----------------------------------------------------------");
    }
    swSerial.println("========= S E T U P =========");
}

// Говорящие переменные, которые особо не относятся к делу 
// и нужны для определения изменения значений в цикле
float temperature;
float waterAmount;
float startTemperature = 0;
unsigned long startDeltaTempCheck;
unsigned long startDeltaMaxTempCheck;
unsigned long dataCheckTimeout = millis();
void loop() 
{
    if (millis() - dataCheckTimeout > 50)
    {
        // Получаем значения температуры и уровня воды
        temperature = getTemperature();
        waterAmount = getWaterAmount();

        // дебаг-сообщения в сериал порт для дебага
        swSerial.print(temperature);
        swSerial.print(' ');
        swSerial.print(waterAmount);
        swSerial.print(' ');
        swSerial.print(temperature - startTemperature);
//        if (temperature > maxTemp)
//        {
//            maxTemp = temperature; 
//        }
        
        if (fabs(temperature - startTemperature) > deltaT)
        {
            startDeltaTempCheck = millis();
            startTemperature = temperature;
            swSerial.print(" [deltaT check has refreshed]");
        }

        if (temperature - maxTemp > deltaMaxT)
        {
            startDeltaMaxTempCheck = millis();
            maxTemp = temperature;
        }

        swSerial.print(' ');
        swSerial.print(maxTemp);

        swSerial.println();

        // обновляем таймаут
        dataCheckTimeout = millis();
    }

    // Отключаемся при достижении максимальной температуры и других условиях
    if(temperature >= maxTemperature && heating)
    {
        bool highTemp = temperature >= minCriticalTemp;
        unsigned long lapsedMax = millis() - startDeltaMaxTempCheck;
        
        unsigned long lapsed = millis() - startDeltaTempCheck;
        if (lapsed > tempTimeout || lapsedMax > maxTempTimeout)
        {
            off();
            sendData('D');
            startTemperature = 0;
            maxTemp = 0;
        }
        else
        {
            swSerial.println("too early!");
        }
    }

    // Обработка входящих сообщений
    if(Serial.available())
    {
        lastData = millis();
        digitalWrite(LED_BUILTIN, HIGH);
        
        char cmd = Serial.read();
        processCommand(cmd);
    }
    else
    {
        digitalWrite(LED_BUILTIN, LOW);
    }
    
    delay(5);
}

void processCommand(char cmd)
{
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

    startDeltaTempCheck = millis();
    startDeltaMaxTempCheck = millis();
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
            if (data > 100 && data <= 105) data = 100;
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

void initDefaultConfig()
{
    config.biasF = biasF;
    config.biasR = biasR;
    config.biasL = biasL;

    config.kF = kF;
    config.kR = kR;
    config.kL = kL;

    EEPROM.put(0, config);
}

void Error(byte id)
{
    byte data[] = {'E', id};
    sendData(data);
    delay(20);
}

