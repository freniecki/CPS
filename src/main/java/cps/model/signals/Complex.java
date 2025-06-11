package cps.model.signals;

import lombok.Setter;

import java.util.Objects;

@Setter
public final class Complex {
    private double real;
    private double imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double real() {
        return real;
    }

    public double imaginary() {
        return imaginary;
    }

    public Complex plus(Complex other) {
        return new Complex(this.real + other.real, this.imaginary + other.imaginary);
    }

    public Complex minus(Complex other) {
        return new Complex(this.real - other.real, this.imaginary - other.imaginary);
    }

    public Complex times(Complex other) {
        return new Complex(this.real * other.real - this.imaginary * other.imaginary,
                this.real * other.imaginary + this.imaginary * other.real);
    }

    @Override
    public String toString() {
        return "Complex{R=%.3f,I=%.3f}".formatted(real, imaginary);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Complex) obj;
        return Double.doubleToLongBits(this.real) == Double.doubleToLongBits(that.real) &&
                Double.doubleToLongBits(this.imaginary) == Double.doubleToLongBits(that.imaginary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(real, imaginary);
    }

}
