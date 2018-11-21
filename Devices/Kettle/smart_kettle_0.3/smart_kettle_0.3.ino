#define PRESS_SENSOR_F A0 // Передний   \|
#define PRESS_SENSOR_L A3 // Левый      || Датчики давления
#define PRESS_SENSOR_R A2 // Правый     /|

#define TEMP_SENSOR A1  // Датчик температуры
#define RELAY 4         // Реле


// Инкапсулирует считывание и сглаживание значений с датчика
class Sensor
{
    public:
        // Коэффициент сглаживания
        double alpha;

        // Конструктор
        Sensor(double alpha, int pin)
        {
            this->alpha = alpha;
            this->pin = pin;
            prevValue = analogRead(pin);
        }

        // Обрабатывает и возвращает значение с датчика
        double getValue()
        {
            // Получаем значение с АЦП
            int value = analogRead(pin);

            // Экспоненциально сглаживаем
            double smoothed = prevValue + alpha * (value - prevValue);

            // Обновляем переменную предыдущего значения
            prevValue = smoothed;

            // Возвращаем обработанное значение
            return smoothed;
        }
        
    private:
        // Сохраненное предыдущее обработанное значение датчика
        double prevValue;
        
        // Пин датчика
        int pin;
};

//-------------------------
// | Параметры термометра |
//-------------------------------
const double room_t0 = 298.15;  // Эталонная комнатная температура (как правило, 25 °C)
const double beta = 4000.0;     // Табличный коэффициент термистора (корректируется эмпирически)
const double balanceR = 325.0;  // Сопротивление резистора делителя напряжения
const double defaultR = 10000.0; // Сопротивление термистора при комнатной температуре
//-------------------------------

//--------------------------------
// | Параметры датчиков давления |
//--------------------------------
const double biasF = 140;   // Смещение. Нужно для вычета веса самого чайника
const double kF = 0.005;     // Коэффициент. Отношение объема к значению с АЦП

const double biasR = 360;   // Для правого датчика
const double kR = 0.007;     // 

const double biasL = 380;   // Для левого датчика
const double kL = 0.007;     // 
//-------------------------------

// ------- Коэффициенты сглаживания для датчиков ------
double temperatureSensorAlpha = 0.02;
double pressureSensorAlphaF = 0.1;  // Передний
double pressureSensorAlphaR = 0.1;  // Правый
double pressureSensorAlphaL = 0.1;  // Левый
// ----------------------------------------------------

// Вкл/выкл отправку сообщений со значениями с датчиков
bool muted = false;

// Режим отладки
bool debug = false;

// Идет ли сейчас нагрев
bool heating = false;

// Переменная для рассчета временных интервалов
unsigned long start = millis();

// Пауза между отправками данных
int sendDataDelay = 64;

// Температура отключения нагревателя (°C)
double maxTemperature = 100;

// Минимальный уровень воды (в литрах)
double minWaterAmount = 0.5;

// Максимальный уровень воды (в литрах)
double maxWaterAmount = 2;


//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// Ручное включение и выключение
void on(bool force = false); // Функция проверяет уровень воды перед включением. При force=true проверка игнорируется
void off();

// Отправка данных о температуре и объеме воды
void sendData(double, double);

// Получение температуры
double getTemperature();

// Получение количества воды
double getWaterAmount();
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\


// Создаем датчики
Sensor temperatureSensor(temperatureSensorAlpha, TEMP_SENSOR);
Sensor pressureSensorF(pressureSensorAlphaF, PRESS_SENSOR_F);
Sensor pressureSensorR(pressureSensorAlphaR, PRESS_SENSOR_R);
Sensor pressureSensorL(pressureSensorAlphaL, PRESS_SENSOR_L);

void setup() 
{
    // Говорим, что реле - это выход
    pinMode(RELAY, OUTPUT);
    // Сразу выключаем реле
    digitalWrite(RELAY, HIGH);

    // Запускаем последовательный порт
    Serial.begin(9600);
    delay(50);
}

void loop() 
{
    // Получаем значения температуры и уровня воды
    double temperature = getTemperature();
    double waterAmount = getWaterAmount();
    // и ждем
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
            
    //Возвращаем температуру в градусах цельсия
    return tKelvin - 273.25;
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

    return amount;
}

void sendData(double tempValue, double waterValue)
{
    // Получаем строковые представления значений с датчиков
    String temperature = String(tempValue);
    String waterAmount = String(waterValue);

    // Строим итоговую строку
    String data = "\nt: " + temperature + " °C\n" +
            "v: " + waterAmount + "L";
    
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

