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

// Массив для обработки входящих данных
char buff[4];

// ID клиента сервера. Обычно это 0
int id = 0;

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
    Serial.begin(115200);
    delay(50);

    // Запуск сервера на только что запущенном ESP8266
    Serial.println("AT+CWMODE=3");  // включаем оба режима AP и STA
    delay(5);
    Serial.println("AT+CIPMUX=1");  // разрешаем множественное подключение
    delay(5);
    Serial.println("AT+CIPSERVER=1,3333");  // запускаем сервер
    delay(10);

    // TODO:
    //  здесь еще можно прочитать входные данные и определить
    //  успешность запуска, но это в следующей версии

    // TODO:
    //  сделать debug как SoftwareSerial на пару пинов
    //  а также метод read() в котором вызывается
    //  и возвращается обычный Serial.read(), 
    //  который дублируется по UART в debug порт  
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
        sendData('D');
    }

    // Обработка входящих сообщений
    if(Serial.available())
    {
        // Проверяем на наличие данных
        if(detectInputData())
        {
            // Здесь мы читаем запятую, которая
            // разделяет команду и следующую цифру
            char c = Serial.read();

            // А здесь можно будет найти длину сообщения
            int length = 0;

            // Читаем первую цифру id подключенного клиента.
            // Почти наверняка эта цифра единственная, но 
            // нужно перестраховаться - читаем до тех пор, 
            // пока не дойдем до запятой
            c = Serial.read();
            while(c != ',')
            {
                // Напоминаю, что id глобальный и пока что это одна переменная,
                // в будущем будет запоминаться весь список активных клиентов
                id *= 10;
                id += c - '0';
                
                c = Serial.read();
            }

            // А здесь мы принимаем первую цифру длины данных.
            // Вот тут уже спокойно может оказаться число
            // больше 9, так что однозначно делаем цикл
            c = Serial.read();
            while(c != ':')
            {
                length *= 10;
                length += c - '0';
                
                c = Serial.read();
            }

            
            
        }
        
    }
}

bool detectInputData()
{
    // Нам нужно обнаружить команду "+IPD", которая 
    // в ESP8266 обозначает наличие входных даных
    char c = Serial.read();
    if ('+' != c)
    {
        return false;
    }
    
    c = Serial.read();
    if ('I' != c)
    {
        return false;
    }
    
    c = Serial.read();
    if ('P' != c)
    {
        return false;
    }
    
    c = Serial.read();
    if ('D' != c)
    {
        return false;
    }

    return true;
}

void processCommand(char cmd)
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

void sendData(char character)
{
    byte data[] = {character, ';', '\n'};
    Serial.println("AT+CIPSEND=" + String(id) + ",3");
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
    Serial.println("AT+CIPSEND=" + String(id) + ',' + String(length + 2));
    Serial.write(data, length);
    Serial.write(';');
    Serial.write('\n');
    Serial.flush();
    delay(10);
}

void Error(byte id)
{
    byte data[] = {'E', id};
    sendData(data);
    delay(20);
}

