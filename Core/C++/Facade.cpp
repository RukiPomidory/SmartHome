#include "Facade.h"

Facade * Facade::instance = 0;
FacadeDestroyer Facade::destroyer;

FacadeDestroyer::~FacadeDestroyer()
{
    delete instance;
}

void FacadeDestroyer::initialize(Facade* p)
{
    instance = p;
}

Facade& Facade::getInstance()
{
    if(!instance)
    {
        instance = new Facade();
        destroyer.initialize(instance);
    }
    return *instance;
}