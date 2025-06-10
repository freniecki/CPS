package cps.model.signals;

public record Complex(double real, double imaginary) {

    public Complex plus(Complex other) {
        return new Complex(this.real + other.real,this.imaginary + other.imaginary);
    }

    public Complex minus(Complex other) {
        return new Complex(this.real - other.real,this.imaginary - other.imaginary);
    }

    public Complex times(Complex other) {
        double real = this.real * other.real - this.imaginary * other.imaginary;
        double imaginary = this.real * other.imaginary + this.imaginary * other.real;
        return new Complex(real, imaginary);
    }

    @Override
    public String toString() {
        return "Complex{R=%.3f,I=%.3f}".formatted(real, imaginary);
    }
}
