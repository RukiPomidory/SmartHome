#include "Device.h"

void Device::setTimeout(int timeout)
{
    if(timeout > 0)
    {
        Device::timeout = timeout;
    }
}