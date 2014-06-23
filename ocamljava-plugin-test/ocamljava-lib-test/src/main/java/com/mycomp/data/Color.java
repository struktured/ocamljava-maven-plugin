package com.mycomp.data;

public class Color {

	private int r;
	public int getR() {
		return r;
	}
	public int getG() {
		return g;
	}
	public int getB() {
		return b;
	}
	private int g;
	private int b;
	public Color(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public Color() {}
	
	@Override
	public String toString() {
		return "Color [r=" + r + ", g=" + g + ", b=" + b + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + b;
		result = prime * result + g;
		result = prime * result + r;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Color other = (Color) obj;
		if (b != other.b)
			return false;
		if (g != other.g)
			return false;
		if (r != other.r)
			return false;
		return true;
	}
	
	
}
