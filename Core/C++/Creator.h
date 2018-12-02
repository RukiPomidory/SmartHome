#ifndef CREATOR_H
#define CREATOR_H

template<typename T>
class Creator
{
public:
    Creator();
    virtual T Create() = 0;
};

#endif