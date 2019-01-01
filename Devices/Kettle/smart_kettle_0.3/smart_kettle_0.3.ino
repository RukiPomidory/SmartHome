#include "Sensor.cpp"
#include "Settings.h"

#define PRESS_SENSOR_F A0 // Передний   \|
#define PRESS_SENSOR_L A3 // Левый      || Датчики давления
#define PRESS_SENSOR_R A2 // Правый     /|

#define TEMP_SENSOR A1  // Датчик температуры
#define RELAY 4         // Реле


// Вкл/выкл отправку сообщений со значениями с датчиков
bool muted = false;

// Режим отладки
bool debug = false;

// Идет ли сейчас нагрев
bool heating = false;


// -------------- Функции --------------
// Включение и выключение нагревателя
void on(bool force = false); // Проверяет уровень воды и если все ок, включает нагрев. 
							// При force=true проверка игнорируется
void off();

// Отправка данных о температуре и объеме воды
void sendData(double, double);

// Получение температуры
double getTemperature();

// Получение количества воды
double getWaterAmount();
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
    if(temperature >= maxTemperature && heating)
    {
        off();
        Serial.println("Вода вскипела!");
    }

    // Если отправка разрешена и прошло достаточно времени, посылаем значения с датчиков
    if(!muted && millis() - start > sendDataDelay)
    {
        sendData(temperature, waterAmount);

        // Обновляем время
        start = millis();
    }

    // Обработка входящих сообщений
    if(Serial.available())
    {
        // Читаем символ
        char c = Serial.read();
        
        if(c == ' ')
        {
            return;
        }

        // Выбираем команду
        switch(c)
        {
            // Включение
            case 'O':
                on();
                break;

            // Увеличение временного интервала
            case '+':
                // Увеличиваем задержку в 2 раза
                sendDataDelay *= 2;
                
                // Сообщаем об изменившейся задержке
                Serial.println("delay time: " + String(sendDataDelay));
                break;

            // Уменьшенье временного интервала
            case '-':
                // Уменьшаем задержку в 2 раза
                sendDataDelay /= 2;

                // При целочисленном делении может получиться 0 - отлавливаем эту ситуацию
                if (sendDataDelay <= 0)
                {
                    sendDataDelay = 1;
                }

                // Сообщаем об изменившейся задержке
                Serial.println("delay time: " + String(sendDataDelay));
                break;

            // Mute - выключение сообщений
            case 'M':
                muted = true;
                Serial.println("muted");
                break;

            // Unmute - включение сообщений
            case 'U':
                muted = false;
                Serial.println("unmuted");
                break;

            // Debug - отладка
            case 'D':
                // Уведомляем о включении или выключении отладки
                if(debug)
                {
                    Serial.println("</debug>");
                    Serial.println("[:>_-_-_RELEASE_-_-_<:]");
                }
                else
                {
                    Serial.println("<debug>");
                }

                // Переворачиваем флаг
                debug = !debug;
                break;
                
            
            // Если команда не распознана - возможно, произошла ошибка. Выключаем чайник.
            default:
                off();
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
            Serial.println("Низкий уровень воды!");
            return;
        }
        else if(water > maxWaterAmount)
        {
            Serial.println("Резервуар переполнен!");
            return;
        }
    }
    
    // Замыкаем реле
    digitalWrite(RELAY, LOW);
    
    // Нагрев пошел
    heating = true;
}

void off()
{
    // Размыкаем реле
    digitalWrite(RELAY, HIGH);
    
    // Нагрев прекращен
    heating = false;
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

    // Датчик у нас инертный, так что придется отсекать слишком большие значения,
    // чтобы температура воды не была больше температуры кипения
    if(tempC > 100 && !debug)
    {
        tempC = 100;
    }
            
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

    // Убираем отрицательные значения, если выключен режим отладки
    if(amount < 0 && !debug)
    {
        amount = 0;
    }

    // Домножаем на общий коэффициент и возвращаем
    return amount * K;
}

void sendData(double tempValue, double waterValue)
{
    // Получаем строковые представления значений с датчиков
    String temperature = String(tempValue, 1);
    String waterAmount = String(waterValue, 1);

    // Строим итоговую строку
    String data = "\nТемпература: " + temperature + " °C\n" +
            "Количество: " + waterAmount + "л";
    
    // Отправляем
    Serial.println(data);

    // Если режим отладки выключен, выходим
    if(!debug)
    {
        return;
    }

    String debugData =  "\nF: " + String(pressureSensorF.getValue()) + 
                        "\nL: " + String(pressureSensorL.getValue()) + 
                        "\nR: " + String(pressureSensorR.getValue()) +
                        "\nTEMP: " + String(temperatureSensor.getValue());

    Serial.println(debugData);
}

