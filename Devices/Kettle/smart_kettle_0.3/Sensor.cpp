#ifndef SENSOR
#define SENSOR
#include <Arduino.h>

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

#endif
