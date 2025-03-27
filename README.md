# CPS 2025
Aplikacja na rzecz realizacji przedmiotu Cyfrowe przetwarzanie sygnału.

## Autorzy

- Franciszek Reniecki, 247773
- Oleksandr Tropets, 245129

## Do poprawy
**Plik**
- zapis — zgodny z formatem w instrukcji
- odczyt

**Sygnały**
- info o częstotliwości próbkowania

**Wybór sygnałów**
- dupa

**UI**
- dostosowanie wyświetlanego fragmentu do rozmiaru ekranu
  - np: dla t=10s, T=0.2s, l.cykli=50 -> za dużo na ekran -> dostosować, aby było widać tylko 2-5 cykli

## Reprezetacja
Każdy sygnał/szum można przedstawić za pomocą:
- amplitudy — maksymalnej wartości odchylenia; zakres przeciwdziedziny
- funkcji czasu — wartości obieranych przez sygnał w czasie jego trwania 
- czasu trwania — przedział dziedziny

Dodatkowe parametry:
- okres podstawowy — dla sygnałów okresowych
- współczynnik wypełnienia — dla sygnału prostokątnego i trójkątnego 

## Sygnały i szumy
parametry: amplitude, startTime, duration 
- (S1) szum o rozkładzie jednostajnym: UNIFORM_NOISE
- (S2) szum gaussowski: GAUSS_NOISE

parametry: amplitude, startTime, duration, period
- (S3) sygnał sinusoidalny: SINE
- (S4) sygnał sinusoidalny wyprostowany jednopołówkowo: SINE_HALF
- (S5) sygnał sinusoidalny wyprostowany dwupołówkowo: SINE_FULL

parametry: amplitude, startTime, duration, period, dutyCycle 
- (S6) sygnał prostokątny: RECTANGLE
- (S7) sygnał prostokątny symetryczny: RECTANGLE_SYMETRIC  
- (S8) sygnał trójkątny: TRIANGLE

parametry: amplitude, startTime, duration, period, stepTime
- (S9) skok jednostkowy: UNIT_STEP

parametry: amplitude, startTime, duration, period, impulseTime
- (S10) impuls jednostkowy: UNIT_IMPULS

parametry: amplitude, startTime, duration, period, probability
- (S11) szum impulsowy: IMPULSE_NOISE

