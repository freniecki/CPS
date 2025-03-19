# CPS 2025
Aplikacja na rzecz realizacji przedmiotu Cyfrowe przetwarzanie sygnału.

## Autorzy

- Franciszek Reniecki, 247773
- Oleksandr Tropets, 245129

## Reprezetacja
Każdy sygnał/szum można przedstawić za pomocą:
- amplitudy — maksymalnej wartości odchylenia; zakres przeciwdziedziny
- funkcji czasu — wartości obieranych przez sygnał w czasie jego trwania 
- czasu trwania — przedział dziedziny

Dodatkowe parametry:
- okres podstawowy — dla sygnałów okresowych
- współczynnik wypełnienia — dla sygnału prostokątnego i trójkątnego 

## Sygnały i szumy
parametry: sampleTime, sampleCount, amplitude
- (S1) szum o rozkładzie jednostajnym: UNIFORM_NOISE
- (S2) szum gaussowski: GAUSS_NOISE

parametry: sampleTime, sampleCount, amplitude, period
- (S3) sygnał sinusoidalny: SINE
- (S4) sygnał sinusoidalny wyprostowany jednopołówkowo: SINE_HALF
- (S5) sygnał sinusoidalny wyprostowany dwupołówkowo: SINE_FULL

parametry: sampleTime, sampleCount, amplitude, period, dutyCycle 
- (S6) sygnał prostokątny: RECTANGLE
- (S7) sygnał prostokątny symetryczny: RECTANGLE_SYMETRIC  
- (S8) sygnał trójkątny: TRIANGLE

parametry: to-be-done
- (S9) skok jednostkowy: UNIT_STEP
- (S10) impuls jednostkowy: UNIT_IMPULS
- (S11) szum impulsowy: IMPULSE_NOISE

