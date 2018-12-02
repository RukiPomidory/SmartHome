#ifndef DEVICE_H
#define DEVICE_H

class Device 
{
public:
    Device();
    virtual int CheckConnection();
    void setTimeout(int);
private:
    int timeout;
};

#endif