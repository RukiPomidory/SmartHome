#ifndef SETTINGS
#define SETTINGS

class Config
{
    public:
    float biasF;
    float biasR;
    float biasL;
    
    float kF;
    float kR;
    float kL;
};


//-------------------------
// | Параметры термометра |
//-------------------------------
const double room_t0 = 298.15;  // Эталонная комнатная температура (как правило, 25 °C)
const double beta = 4650.0;     // Табличный коэффициент термистора (корректируется эмпирически)
const double balanceR = 330.0;  // Сопротивление резистора делителя напряжения
const double defaultR = 10000.0; // Сопротивление термистора при комнатной температуре
//--^--^--^--^--^--^--^--^--^--^-


//--------------------------------
// | Параметры датчиков давления по умолчанию
//--------------------------------
float biasF = 150;     // Смещение. Нужно для вычета веса самого чайника
float kF = 0.0077;    // Коэффициент. Отношение объема к значению с АЦП

float biasR = 850;     // Для правого датчика
float kR = 0.025;     // 

float biasL = 850;    // Для левого датчика
float kL = 0.025;    // 
//--^--^--^--^--^--^--^--^--^--^--


// ------- Коэффициенты сглаживания для датчиков ------
double temperatureSensorAlpha = 0.02;
double pressureSensorAlphaF = 0.1;  // Передний
double pressureSensorAlphaR = 0.1;  // Правый
double pressureSensorAlphaL = 0.1;  // Левый
// --^--^--^--^--^--^--^--^--^--^--^--^--^--^--^--^--^-

// Пауза между отправками данных
int sendDataDelay = 256;

// Температура отключения нагревателя (°C)
double maxTemperature = 100;

// Минимальный уровень воды (в литрах)
double minWaterAmount = 0.5;

// Максимальный уровень воды (в литрах)
double maxWaterAmount = 2;

#endif
