class A {
	private static int x;
	private int y;
	private static int z;
	private int w;
	
	public static void main(String[] args) {
		A a = new A();
		a.dostuff();
		a.dostuff();
		a.dostuff();
		a.dostuff();
	}
	
	private void dostuff() {
		x = 48;
		System.out.println(x);
		y = 49;
		System.out.println(y);
		z = 50;
		System.out.println(z);
		w = 51;
		System.out.println(w);
	}
}