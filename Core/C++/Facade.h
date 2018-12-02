#ifndef FACADE_H
#define FACADE_H

class Facade; // Опережающее объявление

class FacadeDestroyer
{
private:
    Facade * instance;
public:
    ~FacadeDestroyer();
    void initialize(Facade* p);
};

class Facade
{
private:
    static Facade * instance;
    static FacadeDestroyer destroyer;
protected:
    Facade() { }
    Facade(const Facade&);
    Facade& operator=(Facade&);
    ~Facade() { }
    friend class FacadeDestroyer;
public:
    static Facade& getInstance();
    
    // "API"
    
};

#endif