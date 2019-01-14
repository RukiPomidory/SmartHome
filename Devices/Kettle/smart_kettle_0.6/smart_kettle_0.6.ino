#include "Sensor.cpp"
#include "Settings.h"
#include <SoftwareSerial.h> // позволяет создать еще один UART-порт на любых пинах ардуино

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

// Количество оставшихся байтов в сообщении
int bytesLeft = 0;

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

// Создаем последовательный порт на выбранных пинах
SoftwareSerial swSerial(swRX, swTX);

// Создаем датчики
Sensor temperatureSensor(temperatureSensorAlpha, TEMP_SENSOR);
Sensor pressureSensorF(pressureSensorAlphaF, PRESS_SENSOR_F);
Sensor pressureSensorR(pressureSensorAlphaR, PRESS_SENSOR_R);
Sensor pressureSensorL(pressureSensorAlphaL, PRESS_SENSOR_L);

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
    delay(50);

    // Запуск сервера на только что запущенном ESP8266
    Serial.println("AT+CWMODE=3");  // включаем оба режима AP и STA
    delay(5);
    Serial.println("AT+CIPMUX=1");  // разрешаем множественное подключение
    delay(5);
    Serial.println("AT+CIPSERVER=1,3333");  // запускаем сервер
    delay(5);
    Serial.println("ATE0"); // отрубаем echo
    delay(10);

    start = millis();

    // TODO:
    //  здесь еще можно прочитать входные данные и определить
    //  успешность запуска, но это в следующей версии
}

void loop() 
{
    double temperature;
    double waterAmount;
    
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
        digitalWrite(LED_BUILTIN, HIGH);
        // Проверяем на наличие данных
        if(detectInputData())
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
        
    }
    else
    {
        digitalWrite(LED_BUILTIN, LOW);
    }
    delay(5);
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

void calibrate()
{
    off();
    
    Error(12);
    
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

    // Здесь у нас сеть без пароля
    if (0 == passLength) 
    {
        String request = "AT+CWJAP_CUR=" + String(ssid) + "\"\""; //Изменяем параметры подключения к точке доступа: ssid в кавычках, запятая и пустой пароль ""
        Serial.println(request);
        Serial.flush();
        return;
    }

    char password[passLength + 3];
    password[0] = '"';
    password[passLength + 1] = '"';
    
    password[passLength + 2] = 0;   // Принудительно выставляем конец строки в стиле C, потому что без этого не работает
    
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

    String request = "AT+CWJAP_CUR=" + String(ssid) + "|" + String(password);
    Serial.println(request);
    Serial.flush();
}



void Error(byte id)
{
    byte data[] = {'E', id};
    sendData(data);
    delay(20);
}

