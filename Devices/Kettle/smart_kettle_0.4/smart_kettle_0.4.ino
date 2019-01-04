#include "Sensor.cpp"
#include "Settings.h"

#define PRESS_SENSOR_F A0 // Передний   \|
#define PRESS_SENSOR_L A3 // Левый      || Датчики давления
#define PRESS_SENSOR_R A2 // Правый     /|

#define TEMP_SENSOR A1  // Датчик температуры
#define RELAY 4         // Реле


// Идет ли сейчас нагрев
bool heating = false;

// Контроль нескончаемого потока данных с датчиков
bool flow = false;

// -------------- Функции --------------
// Включение и выключение нагревателя
void on(bool force = false); // Проверяет уровень воды и если все ок, включает нагрев. 
							// При force=true проверка игнорируется
void off();

// Получение температуры
double getTemperature();

// Получение количества воды
double getWaterAmount();

// Отправка значения с указанного датчика
void sendSensorData();

// Отправка сообщения о конкретной ошибке
void Error(byte);
//--^--^--^--^--^--^--^--^--^--^--^--^--


// Создаем датчики
Sensor temperatureSensor(temperatureSensorAlpha, TEMP_SENSOR);
Sensor pressureSensorF(pressureSensorAlphaF, PRESS_SENSOR_F);
Sensor pressureSensorR(pressureSensorAlphaR, PRESS_SENSOR_R);
Sensor pressureSensorL(pressureSensorAlphaL, PRESS_SENSOR_L);

void setup() 
{
    // Выставляем режим OUTPUT на реле и сразу отключаем
    pinMode(RELAY, OUTPUT);
    digitalWrite(RELAY, HIGH);

    // Запускаем последовательный порт (debug)
    Serial.begin(9600);
    delay(50);
}

void loop() 
{
    // Получаем значения температуры и уровня воды
    double temperature = getTemperature();
    double waterAmount = getWaterAmount();
    delay(50);

    // Отключаемся при достижении максимальной температуры
    if(round(temperature) >= maxTemperature && heating)
    {
        off();
        Serial.print("D");
    }

    // Обработка входящих сообщений
    if(Serial.available())
    {
        // Читаем символ
        char c = Serial.read();

        // Выбираем команду
        switch(c)
        {
            // Включение
            case 'H':
                on();
                break;

            // Проверка связи
            case 'A':
                Serial.write(0x41);
                break;
            
            // Выключение
            case 'K':
                off();
                break;

            // Запрос значения с датчика
            case 'R':
                sendSensorData();
                break;

            // Калибровка датчиков
            case 'C':
                calibrate();
                break;

            // Вкл/Выкл потока информации с датчиков
            case 'F':
                flowHandler();
                break;
            
            default:
                Error(11);
                break;
        }
    }
}

void on(bool force = false)
{
    if(!force)
    {
        // Проверка уровня воды
        double water = getWaterAmount();
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
    }
    
    // Замыкаем реле
    digitalWrite(RELAY, LOW);
    
    // Нагрев пошел
    heating = true;

    // Отправляем подтверждение включения
    Serial.print("H");
}

void off()
{
    // Размыкаем реле
    digitalWrite(RELAY, HIGH);
    
    // Нагрев прекращен
    heating = false;

    // Отправляем подтверждение выключения
    Serial.print("K");
}

double getTemperature()
{
    // Получаем значение с АЦП и ждем
    double value = temperatureSensor.getValue();
    delay(10);
    
    // Вычисляем текущее сопротивление термистора
    double resistance = balanceR * (1023.0 / value - 1);

    // Получаем температуру в кельвинах, 
    // используя перестроенное бета-уравнение
    double tKelvin = (beta * room_t0) / 
            (beta + (room_t0 * log(resistance / defaultR)));

    // Температура в градусах цельсия
    double tempC = tKelvin - 273.15;
            
    //Возвращаем температуру в градусах цельсия
    return tempC;
}

double getWaterAmount()
{
    // Получаем значения с датчиков
    double valueF = pressureSensorF.getValue();
    double valueR = pressureSensorR.getValue();
    double valueL = pressureSensorL.getValue();

    // Считаем уровень воды отдельно для каждого
    double amountF = (valueF - biasF) * kF;
    double amountR = (valueR - biasR) * kR;
    double amountL = (valueL - biasL) * kL;

    // Берем среднее арифметическое
    double amount = (amountF + amountR + amountL) / 3;

    // Убираем отрицательные значения
    if(amount < 0)
    {
        amount = 0;
    }

    // Домножаем на общий коэффициент и возвращаем
    return amount * K;
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
            break;

        default:
            Error(10);
            return;
    }

    byte buf[5];
    buf[0] = 'T';
    buf[1] = id;
    
    if (data > 256)
    {
        buf[2] = 1;
        buf[3] = data / 256;
        buf[4] = data % 256;
        Serial.write(buf, 5);
    }
    else
    {
        buf[2] = 0;
        buf[3] = data;
        Serial.write(buf, 4);
    }
}

void calibrate()
{
    off();
    
    Error(12);
    
}

void flowHandler()
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

void Error(byte id)
{
    Serial.write('E');
    Serial.write(id);
    delay(20);
}

