#ifndef IN_CREATOR_H
#define IN_CREATOR_H
#include "Creator.h"
#include "Device.h"

class InCreator : public Creator<Device>
{
public:
    InCreator();
    Device Create();
};

#endif